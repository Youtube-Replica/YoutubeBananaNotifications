package model;

import Client.Client;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.entity.BaseDocument;
import com.arangodb.util.MapBuilder;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Map;

public class Notification {

    static ArangoDB arangoDB;
    static Notification instance = new Notification();
    static String dbName = "subscriptions";

    private Notification(){
        arangoDB = new ArangoDB.Builder().build();
    }

    public static Notification getInstance(){
        return Notification.instance;
    }

    public void setDB(int i){
        arangoDB = new ArangoDB.Builder().maxConnections(i).build();
    }

    //Notification for Videos by title and for Channel by name
    public static String getSearch(String s) {
        //First get by channel name
        JSONObject searchObjectTotal = new JSONObject();

            String query = "FOR doc IN Comments\n" +
                    "        FILTER doc.video_id == @value\n" +
//                    "        FILTER CONTAINS(doc.video_id, @value)" +
                    "        RETURN doc";
            Map<String, Object> bindVars = new MapBuilder().put("value", s).get();

            ArangoCursor<BaseDocument> cursor = arangoDB.db(dbName).query(query, bindVars, null,
                    BaseDocument.class);
            if(cursor.hasNext()) {
                BaseDocument cursor2 = null;
                JSONArray searchArray = new JSONArray();
                int id = 0;
                for (; cursor.hasNext(); ) {
                    JSONObject commentObjectM = new JSONObject();
                    cursor2 = cursor.next();
                    try {
                        BaseDocument myDocument = arangoDB.db(dbName).collection("Comments").getDocument(cursor2.getKey(),
                                BaseDocument.class);
                        id = Integer.parseInt(cursor2.getKey());
                        commentObjectM.put("Text",myDocument.getAttribute("text"));
                        searchArray.add(commentObjectM);
                    } catch (ArangoDBException e) {
                        Client.serverChannel.writeAndFlush(Unpooled.copiedBuffer("Error> Failed to get document: myKey; " + e.getMessage(), CharsetUtil.UTF_8));
                        System.err.println("Failed to get document: myKey; " + e.getMessage());
                    }
                }
                searchObjectTotal.put("Comments", searchArray);
            }

                JSONArray searchArray = new JSONArray();
                query = "FOR doc IN firstSubscription\n" +
                       // "        FILTER doc.`title` like @value\n" +
                        "        FILTER CONTAINS(doc.ID, @value)" +
                        "        RETURN doc";
                bindVars = new MapBuilder().put("value", s).get();

                cursor = arangoDB.db(dbName).query(query, bindVars, null,
                        BaseDocument.class);

                if(cursor.hasNext()) {
                    BaseDocument cursor2=null;
                    int id=0;
                    for (; cursor.hasNext(); ) {
                        cursor2 = cursor.next();
                        System.out.println(cursor2.getKey());
                        JSONObject searchObjectM = new JSONObject();
                        try{
                        BaseDocument myDocument2 = arangoDB.db(dbName).collection("firstSubscription").getDocument(cursor2.getKey(),
                                BaseDocument.class);
                        id= Integer.parseInt(cursor2.getKey());

                        searchObjectM.put("ID",id);

                        searchArray.add(searchObjectM);
                    } catch (ArangoDBException e) {
                        Client.serverChannel.writeAndFlush(Unpooled.copiedBuffer("Error> Failed to get document: myKey; " + e.getMessage(), CharsetUtil.UTF_8));
                        System.err.println("Failed to get document: myKey; " + e.getMessage());
                     }
                    }
                    searchObjectTotal.put("subscriptions",searchArray);
                }
                else{
                    Client.serverChannel.writeAndFlush(Unpooled.copiedBuffer("Error> No entry by that name " + s, CharsetUtil.UTF_8));
                    searchObjectTotal.put("No Entry by the name ",s);
                }

        System.out.println("Notification Object" + searchObjectTotal.toString());
        return searchObjectTotal.toString();
    }

}
