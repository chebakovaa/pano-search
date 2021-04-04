package com.concordsoft.naviws;

import com.concordsoft.naviws.model.NaviNode;
import com.concordsoft.naviws.model.SearchMessage;
import com.google.gson.Gson;
import org.neo4j.driver.*;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.neo4j.driver.Values.parameters;


public class WebSocketHandler extends AbstractWebSocketHandler {

    static private final String SERVER_URI = "bolt://localhost:7687";
    static private final String SERVER_USERNAME = "neo4j";
    static private final String SERVER_PASSWORD = "admin";

    static private final Driver driver = GraphDatabase.driver(SERVER_URI, AuthTokens.basic( SERVER_USERNAME, SERVER_PASSWORD ));

    @Override
    protected void handleTextMessage(WebSocketSession sessionWS, TextMessage message) throws IOException, InterruptedException {

        Gson g = new Gson();
        SearchMessage obj = g.fromJson(message.getPayload(), SearchMessage.class);
        String sText = "*" + obj.getText().replace(' ', '*') + "*";
        String pid;
        if (obj.getParent().id.length() > 0) {
            pid = obj.getParent().id;
        } else {
            pid = obj.getParent().pid; //
        }
        try ( Session session = driver.session() ) {
            String greeting = session.readTransaction( new TransactionWork<String>()
            {
                @Override
                public String execute( Transaction tx )
                {
                    for(int indexPath = 1; indexPath < 4; indexPath++) {
                        String cypher = "CALL db.index.fulltext.queryNodes('nameSearch', $text) YIELD node as obj " +
                                //                            "RETURN {id:obj.uid, mnem:labels(obj), name: obj.name, pid: ''} ";
                                "MATCH p=((root {uid: $pid})<-[:CONTAINED_INTO*" + indexPath + ".." + indexPath + "]-(obj)) " +
                                "WHERE ALL(n1 IN nodes(p) WHERE size([n2 IN nodes(p) WHERE labels(n1) = labels(n2)]) = 1) " +
                                "RETURN [v IN nodes(p)|v{id:v.uid, mnem:labels(v), .name, pid: ''}] " +
                                "LIMIT 40";
                        Result result = tx.run(cypher, parameters("text", sText, "pid", pid));
                        while (result.hasNext()) {
                            Record obj1 = result.next();
                            Value obj2 = obj1.get(0);
                            List<NaviNode> nnL = new ArrayList<NaviNode>();
                            for (Value obj3 : obj2.values()) {
                                NaviNode nn = new NaviNode();
                                nn.name = obj3.get("name").asString();
                                nn.pid = obj3.get("pid").asString();
                                nn.id = obj3.get("id").asString();
                                ArrayList<String> amnems = new ArrayList();
                                for (Value mnems : obj3.get("mnem").values()) {
                                    amnems.add(mnems.asString());
                                }
                                nn.mnem = amnems.toArray(new String[0]);
                                nnL.add(nn);
                            }
                            String gson = g.toJson(nnL);
                            try {
                                sessionWS.sendMessage(new TextMessage(gson));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    try {
                        sessionWS.sendMessage(new TextMessage(""));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return "test";
                }
            } );
//            System.out.println( greeting );
        }
        // driver.close();
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws IOException {
        System.out.println("New Binary Message Received");
        session.sendMessage(message);
    }
}
