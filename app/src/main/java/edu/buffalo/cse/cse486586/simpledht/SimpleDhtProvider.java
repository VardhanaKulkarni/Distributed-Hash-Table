package edu.buffalo.cse.cse486586.simpledht;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.database.MatrixCursor;
import android.text.Selection;
import android.util.Log;
import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import android.os.AsyncTask;
import java.net.Socket;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Collections;


public class SimpleDhtProvider extends ContentProvider {

    static final String TAG = SimpleDhtProvider.class.getSimpleName();
    static final String[] REMOTE_PORT = {"11108","11112","11116","11120","11124"};
    static final int SERVER_PORT = 10000;
    static  String MYPort = "";
    static String succ_node = null;
    static  String prev_node = null;
    static String initNode = null;
    static Context context = null;
    static String retmsg = "";
    static Integer count = 1;

    static HashMap<String,Integer> map = new HashMap<String, Integer>();

    class myObj{
        String port;
        String Hashport;
        public myObj(String Port,String Hport){
            this.port = Port;
            this.Hashport = Hport;
        }
    }

    class compp implements Comparator<myObj> {
            @Override
            public int compare(myObj lhs, myObj rhs) {
                if (lhs.Hashport.compareTo(rhs.Hashport) < 0)
                    return -1;
                if (rhs.Hashport.compareTo(lhs.Hashport) > 0)
                    return 1;
                return 0;
        };
        }

    public void initHashMap(){
        for(int i=0;i<5;i++){
            if(i==0){
                map.put(REMOTE_PORT[i],1);
            }else{
                map.put(REMOTE_PORT[i],0);
            }
        }
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        try {
            String Path = context.getFilesDir().getAbsolutePath();
            if (selection.equals("@")) {

                File file1 = new File(Path);
                File[] fileNames = file1.listFiles();
               
                for (int i = 0; i < fileNames.length; i++) {
                    String FileN = fileNames[i].getName();

                    File filex = new File(Path, FileN);
                    filex.delete();
                }
            } else if (selection.equals("*")) {
                if(prev_node!=null && succ_node != null) {

                    Socket sockett = new Socket();
                    sockett.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(succ_node)));
                    PrintWriter out = new PrintWriter(sockett.getOutputStream());
                    BufferedReader in = new BufferedReader(new InputStreamReader(sockett.getInputStream()));
                    String msgstar = "DELETESTAR" + "%" + MYPort;
                   
                    out.println(msgstar);
                    out.flush();
                    String Final = in.readLine();
                    String localMsgs = deleteAllMsgs();
                }else{
                    File file1 = new File(Path);
                    File[] fileNames = file1.listFiles();
                    for (int i = 0; i < fileNames.length; i++) {      
                        String FileN = fileNames[i].getName();
                        File filex = new File(Path, FileN);
                  
                        BufferedReader readFile = new BufferedReader(new FileReader(filex));
                        String value = readFile.readLine();
                        readFile.close();
                    }
                }

            } else {
                if (prev_node != null && succ_node != null) {
                    String hashSelection = genHash(selection);
                    String hashOwnnode = genHash(Integer.toString(Integer.parseInt(MYPort) / 2));
                    String hashPrevnode = genHash(Integer.toString(Integer.parseInt(prev_node) / 2));


                    if ((MYPort.equals((initNode)) && ((hashSelection.compareTo(hashOwnnode) <= 0) || (hashSelection.compareTo(hashPrevnode) > 0))) ||
                            (!MYPort.equals((initNode)) && ((hashSelection.compareTo(hashOwnnode) <= 0) && (hashSelection.compareTo(hashPrevnode) > 0)))) {

                        File file = new File(Path, selection);
                        file.delete();
                    } else {
                        Socket socket = new Socket();
                        socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(succ_node)));
                        PrintWriter out = new PrintWriter(socket.getOutputStream());
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        out.println("DELETE" + "%" + selection + "%" + MYPort);
                        out.flush();
                        String Rmsg = in.readLine();
                    }
                } else {
                    File file = new File(Path, selection);
                   file.delete();
                }
            }

        }catch(Exception e){
            Log.e(TAG,"EXCeption");
        }
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public Uri insert(Uri uri, ContentValues values) {
       
        String name = values.getAsString("key");
        String msg = values.getAsString("value");
        
        try {
            String hashedKey = genHash(name);

            if (succ_node == null) {
                storeContentProvider(name,msg,context);
            } else if (succ_node != null && prev_node!= null) {

                String hashOwnnode = genHash(Integer.toString(Integer.parseInt(MYPort) / 2));
                String hashPrevnode = genHash(Integer.toString(Integer.parseInt(prev_node)/ 2));
                
                if(MYPort.equals(initNode)){
                    if((hashedKey.compareTo(hashOwnnode)<=0) || (hashedKey.compareTo(hashPrevnode)>0)){
                        storeContentProvider(name, msg,context);
                    }else{
                        Socket socket = new Socket();
                        socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(succ_node)));
                        PrintWriter out = new PrintWriter(socket.getOutputStream());
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        out.write("CHECK"+"%"+name+"%"+msg);
                        out.flush();
                        out.close();
                    }
                }else {
                    if ((hashedKey.compareTo(hashOwnnode) <= 0) && (hashedKey.compareTo(hashPrevnode) > 0)) {
                        storeContentProvider(name, msg,context);
                    } else {
                        Socket socket = new Socket();
                        socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(succ_node)));
                        PrintWriter out = new PrintWriter(socket.getOutputStream());
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        out.write("CHECK" + "%" + name + "%" + msg);
                        out.flush();
                        out.close();
                    }
                }
            }
        }catch (NoSuchAlgorithmException e){
            Log.e(TAG,"Expection Generated");
        }catch(UnknownHostException e){
            Log.e(TAG,"UnknownHost Exception");
        }catch(IOException e){
            Log.e(TAG,"IO Exceptiion");
        }
        return uri;
    }

    public void storeContentProvider(String name,String msg,Context context1){
        try {
            FileOutputStream fos;
           
            fos = context1.openFileOutput(name, context1.MODE_PRIVATE);
            fos.write(msg.getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, " IOException");
        }catch (Exception e){
            Log.e("TAG","Just some exception" +"   "+e.toString());
        }
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        return false;
    }

    public String getAllMsgs()
    {
        String Path = context.getFilesDir().getAbsolutePath();
        File file1 = new File(Path);
        File[] fileNames = file1.listFiles();
        String result = "";

        try {
            for (int i = 0; i < fileNames.length; i++) {
                String FileN = fileNames[i].getName();

                File filex = new File(Path, FileN);
                BufferedReader readFile = new BufferedReader(new FileReader(filex));
                String value = readFile.readLine();
                readFile.close();
                result += FileN + "-" + value + "%";
            }
        } catch (Exception e) {
            Log.e("exception", e.toString());
        }
        return result;
    }

    public String deleteAllMsgs()
    {
        String Path = context.getFilesDir().getAbsolutePath();
        File file1 = new File(Path);
        File[] fileNames = file1.listFiles();
        String result = "";

        try {
            for (int i = 0; i < fileNames.length; i++) {
                String FileN = fileNames[i].getName();

                File filex = new File(Path, FileN);
                filex.delete();

            }
        } catch (Exception e) {
            Log.e("exception", e.toString());
        }
        return result;
    }



    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        MatrixCursor cursor = new MatrixCursor(new String[]{"key","value"});
        String Path = context.getFilesDir().getAbsolutePath();

        try {
           if(selection.equals("@") ) {

               File file1 = new File(Path);
               File[] fileNames = file1.listFiles();
               for (int i = 0; i < fileNames.length; i++) {
                   String FileN = fileNames[i].getName();
                   File filex = new File(Path, FileN);
                  
                   BufferedReader readFile = new BufferedReader(new FileReader(filex));
                   String value = readFile.readLine();
                   readFile.close();

                   cursor.addRow(new Object[]{FileN, value});
               }
           }else if(selection.equals("*")){
               if(prev_node!=null && succ_node != null) {

                   Socket sockett = new Socket();
                   sockett.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                           Integer.parseInt(succ_node)));
                   PrintWriter out = new PrintWriter(sockett.getOutputStream());
                   BufferedReader in = new BufferedReader(new InputStreamReader(sockett.getInputStream()));
                   String msgstar = "STAR" + "%" + MYPort;
                   
                   out.println(msgstar);
                   out.flush();
                   String Final = in.readLine();
                   String localMsgs = getAllMsgs();
                   cursor = constructCursor(localMsgs + Final);
               }else{
                   File file1 = new File(Path);
                   File[] fileNames = file1.listFiles();
                   
                   for (int i = 0; i < fileNames.length; i++) {
                       String FileN = fileNames[i].getName();
                       File filex = new File(Path, FileN);
                      
                       BufferedReader readFile = new BufferedReader(new FileReader(filex));
                       String value = readFile.readLine();
                       readFile.close();
                       cursor.addRow(new Object[]{FileN, value});
                   }
               }
           }else{
               if(prev_node!=null && succ_node != null) {
                   String hashSelection = genHash(selection);
                   String hashOwnnode = genHash(Integer.toString(Integer.parseInt(MYPort) / 2));
                   String hashPrevnode = genHash(Integer.toString(Integer.parseInt(prev_node) / 2));


                   if ((MYPort.equals((initNode))&& ((hashSelection.compareTo(hashOwnnode) <= 0) || (hashSelection.compareTo(hashPrevnode) > 0))) ||
                           (!MYPort.equals((initNode))&& ((hashSelection.compareTo(hashOwnnode) <= 0) && (hashSelection.compareTo(hashPrevnode) > 0))) ){

                       File file = new File(Path, selection);
                           BufferedReader readFile = new BufferedReader(new FileReader(file));
                           String value = readFile.readLine();
                           readFile.close();
                           cursor.addRow(new Object[]{selection, value});
                       } else {
                           Socket socket = new Socket();
                           socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                   Integer.parseInt(succ_node)));
                           PrintWriter out = new PrintWriter(socket.getOutputStream());
                           BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                           out.println("QUERY" + "%" + selection + "%" + MYPort);
                           out.flush();
                           
                           String Rmsg = in.readLine();
                           String[] RmsgArray = Rmsg.split("%");
                           cursor.addRow(new Object[]{RmsgArray[0], RmsgArray[1]});
                       }
               }else{
                   File file = new File(Path, selection);
                   BufferedReader readFile = new BufferedReader(new FileReader(file));
                   String value = readFile.readLine();
                   readFile.close();
                   cursor.addRow(new Object[]{selection, value});
               }
           }
        }catch (Exception e){
            Log.e(TAG, " IOException "+ e.toString());
        }
        return cursor;
    }

    public void prevForward(String Rmsg){
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                    Integer.parseInt(prev_node)));
            PrintWriter out1 = new PrintWriter(socket.getOutputStream());
            BufferedReader in1 = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out1.write(Rmsg);
        }catch(IOException e){
            Log.e(TAG,"IO Exception");
        }
    }

    public MatrixCursor constructCursor(String Final){
        String[] FinaArray = Final.split("%");
        MatrixCursor cursor = new MatrixCursor(new String[]{"key","value"});
        for(int i=0;i< FinaArray.length;i++){
            String[] sub = FinaArray[i].split("-");
            String key = sub[0];
            String val = sub[1];
            cursor.addRow(new Object[]{key, val});
        }
        return cursor;
    }

    public String queryForward(Uri uurrii,String selection, String origPort){
        String Path = context.getFilesDir().getAbsolutePath();
        try {
            String hashSelection = genHash(selection);
            String hashOwnnode = genHash(Integer.toString(Integer.parseInt(MYPort) / 2));
            String hashPrevnode = genHash(Integer.toString(Integer.parseInt(prev_node) / 2));

                if ((MYPort.equals((initNode))&& ((hashSelection.compareTo(hashOwnnode) <= 0) || (hashSelection.compareTo(hashPrevnode) > 0))) ||
                 (!MYPort.equals((initNode))&& ((hashSelection.compareTo(hashOwnnode) <= 0) && (hashSelection.compareTo(hashPrevnode) > 0))) ){
                    File file = new File(Path, selection);
                    BufferedReader readFile = new BufferedReader(new FileReader(file));
                    String value = readFile.readLine();
                    readFile.close();
                    return value;
                } else {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(succ_node)));
                    PrintWriter out = new PrintWriter(socket.getOutputStream());
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out.println("QUERY" + "%" + selection + "%" + origPort);
                    out.flush();
                    String Rmsg = in.readLine();
                    
                    String[] RmsgArray = Rmsg.split("%");
                    return RmsgArray[1];
                }
            }catch(NoSuchAlgorithmException e){
            Log.e("from QueryForward","No such algorithm");
        }catch (IOException e){
            Log.e(TAG,"Io");
        }
        return null;
    }

    public String deleteForward(Uri uurrii,String selection, String origPort){
        String Path = context.getFilesDir().getAbsolutePath();
        try {
            String hashSelection = genHash(selection);
            String hashOwnnode = genHash(Integer.toString(Integer.parseInt(MYPort) / 2));
            String hashPrevnode = genHash(Integer.toString(Integer.parseInt(prev_node) / 2));

            if ((MYPort.equals((initNode))&& ((hashSelection.compareTo(hashOwnnode) <= 0) || (hashSelection.compareTo(hashPrevnode) > 0))) ||
                    (!MYPort.equals((initNode))&& ((hashSelection.compareTo(hashOwnnode) <= 0) && (hashSelection.compareTo(hashPrevnode) > 0))) ){
                File file = new File(Path, selection);
                file.delete();
                return "DELETE-COMPLETE";
            } else {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(succ_node)));
                PrintWriter out = new PrintWriter(socket.getOutputStream());
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out.println("DELETE" + "%" + selection + "%" + origPort);
                out.flush();
                String Rmsg = in.readLine();
                return Rmsg;
            }
        }catch(NoSuchAlgorithmException e){
            Log.e("from QueryForward","No such algorithm");
        }catch (IOException e){
            Log.e(TAG,"Io");
        }
        return null;
    }

    public String queryForwardAll(String Origport){
        try {
            String Path = context.getFilesDir().getAbsolutePath();
            if (Origport.equals(succ_node)) {
                File file1 = new File(Path);
                File[] fileNames = file1.listFiles();
                String toSend = "";

                for (int i = 0; i < fileNames.length; i++) {
                    String FileN = fileNames[i].getName();
                    File filex = new File(Path, FileN);
                    BufferedReader readFile = new BufferedReader(new FileReader(filex));
                    String value = readFile.readLine();
                    readFile.close();
                    toSend = toSend + FileN + "-" + value + "%";
                }
                return toSend;
            } else {
                String msgp = "STAR" + "%" + Origport;
                Socket sockett = new Socket();
                sockett.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(succ_node)));
                PrintWriter out = new PrintWriter(sockett.getOutputStream());
                BufferedReader in = new BufferedReader(new InputStreamReader(sockett.getInputStream()));
                out.println(msgp);
                out.flush();
                String msgreceived = in.readLine();
                File file1 = new File(Path);
                File[] fileNames = file1.listFiles();
               
                for (int i = 0; i < fileNames.length; i++) {
                    String FileN = fileNames[i].getName();

                    File filex = new File(Path, FileN);
                    BufferedReader readFile = new BufferedReader(new FileReader(filex));
                    String value = readFile.readLine();
                    readFile.close();
                    msgreceived = msgreceived + FileN + "-" + value + "%";

                }
                return msgreceived;
            }
        }catch (IOException e){
            Log.e(TAG,"IO excetion");
        }
        return  null;
    }



    public String deleteForwardAll(String Origport) {
        try {
            String Path = context.getFilesDir().getAbsolutePath();
            if (Origport.equals(succ_node)) {
                File file1 = new File(Path);
                File[] fileNames = file1.listFiles();
                String toSend = "";

                for (int i = 0; i < fileNames.length; i++) {
                    String FileN = fileNames[i].getName();

                    File filex = new File(Path, FileN);
                    filex.delete();
                    toSend = toSend;
                    return toSend;
                }
                }else{
                    String msgp = "DELETESTAR" + "%" + Origport;
                    Socket sockett = new Socket();
                    sockett.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(succ_node)));
                    PrintWriter out = new PrintWriter(sockett.getOutputStream());
                    BufferedReader in = new BufferedReader(new InputStreamReader(sockett.getInputStream()));
                    out.println(msgp);
                    out.flush();
                    String msgreceived = in.readLine();

                    File file1 = new File(Path);
                    File[] fileNames = file1.listFiles();
                    
                    for (int i = 0; i < fileNames.length; i++) {
                        String FileN = fileNames[i].getName();

                        File filex = new File(Path, FileN);
                        filex.delete();
                        msgreceived = msgreceived;

                    }
                    return msgreceived;
                }
            }catch(IOException e){
                Log.e(TAG, "IO excetion");
            }
            return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    public void socketCreate(String myport,Context contextt){
        try {
            /* Create a server socket as well as a thread (AsyncTask) that listens on the server
            * port.*/
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT,100);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
            MYPort = myport;
            context = contextt;
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            try {
                while (true){
                    Socket Csocket = serverSocket.accept();
                    Log.e("from server","connected" + MYPort);
                    BufferedReader in = new BufferedReader(new InputStreamReader(Csocket.getInputStream()));
                    PrintWriter out = new PrintWriter(Csocket.getOutputStream());
                    String msgReceived ="";
                    String unOrderedList = "";
                    String ordderedList = "";
                    LinkedList<myObj> list =new LinkedList<myObj>();
                    msgReceived = in.readLine();
                    String[] msgR = msgReceived.split("%");

                    if(msgR[0].equals("JOIN")){
                        String JoinedPort = msgR[1];
                        map.put(msgR[1], 1);

                        for(int i=0;i<5;i++){
                            if(map.get(REMOTE_PORT[i])==1){
                                String Hport = genHash(Integer.toString(Integer.parseInt(REMOTE_PORT[i])/ 2));
                                list.add(new myObj(REMOTE_PORT[i],Hport));
                            }
                        }
                        Collections.sort(list,new compp());

                        String sendString = "ORDER";

                        Iterator<myObj> it = list.iterator();
                        while(it.hasNext()){
                            myObj cur = it.next();
                            sendString = sendString +"%"+cur.port;
                        }

                        Socket sockett[] = new Socket[5];
                        PrintWriter[] outt = new PrintWriter[5];
                        BufferedReader[] inn = new BufferedReader[5];

                        for(int k=0;k<5;k++){
                            if(map.get(REMOTE_PORT[k])==1){
                                sockett[k] = new Socket();
                                sockett[k].connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                        Integer.parseInt(REMOTE_PORT[k])));
                                outt[k] = new PrintWriter(sockett[k].getOutputStream());
                                inn[k] = new BufferedReader(new InputStreamReader(sockett[k].getInputStream()));
                                outt[k].println(sendString);
                                outt[k].flush();
                                outt[k].close();
                            }
                        }
                    }

                    if(msgR[0].equals("ORDER")){
                        String msgOrder = msgReceived;
                        String[] msgO = msgOrder.split("%");

                        for(int l=1;l<msgO.length;l++){
                            if(MYPort.equals(msgO[l])){
                                if(l==1){
                                    prev_node = msgO[msgO.length-1];
                                    succ_node = msgO[l+1];
                                }else if(l == (msgO.length-1)){
                                    prev_node = msgO[l-1];
                                    succ_node = msgO[1];
                                }else{
                                    prev_node = msgO[l-1];
                                    succ_node = msgO[l+1];
                                }
                            }
                            initNode = msgO[1];
                        }
                   
                    }

                    if(msgR[0].equals("CHECK")){
                        String value1 = msgR[1];
                        String value2 = msgR[2];

                        ContentValues values = new ContentValues();
                        values.put("key", value1);
                        values.put("value", value2);
                        Uri uurrii = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");
                        insert(uurrii,values);
                    }

                    if(msgR[0].equals("QUERY")){
                        String selec = msgR[1];
                       // Integer tcount = Integer.valueOf(msgR[2]);
                        Uri uurrii = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");
                        String reply = queryForward(uurrii, selec,msgR[2]);
                        out.println(selec+"%"+reply+"%"+msgR[2]);
                        out.flush();
                        out.close();
                    }
                    if(msgR[0].equals("STAR")){
                        String origPort = msgR[1];
                        String res = queryForwardAll(origPort);
                        String[] Resarray = res.split("%");
                        out.println(res);
                        out.flush();
                    }

                    if(msgR[0].equals("DELETESTAR")){
                        String origPort = msgR[1];
                        String res = queryForwardAll(origPort);
                        out.println(res);
                        out.flush();
                    }

                    if(msgR[0].equals("DELETE")){
                        String selec = msgR[1];
                        // Integer tcount = Integer.valueOf(msgR[2]);
                        Uri uurrii = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");
                        String reply = deleteForward(uurrii, selec, msgR[2]);
                        out.println(reply);
                        out.flush();
                        out.close();
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "serverTask socket IOException");
            }catch(NoSuchAlgorithmException e){
                Log.e(TAG,"Exception");
            }
            return null;
        }
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
}
