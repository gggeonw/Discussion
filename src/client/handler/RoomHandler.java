package client.handler;

import java.io.PrintWriter;

import static client.dto.RequestCommand.*;

public class RoomHandler {
    private PrintWriter pw;

    public RoomHandler(PrintWriter pw) {
        this.pw = pw;
    }

    public void getRoomList(){
        pw.println(GET_ROOM_LIST.name());
        pw.flush();
    }

    public void findRoom(String roomName){
        String request = FIND_ROOM.name() + " " + roomName;
        pw.println(request);
        pw.flush();
    }

    public void createRoom(String topic, String firstStatus, String secondStatus, String nickname){
        String request = CREATE_ROOM.name() + " " +topic + " " + firstStatus + " " + secondStatus + " " + nickname;
        pw.println(request);
        pw.flush();
    }

    public void enterRoom(String roomName, String status){
        String request = ENTER_ROOM.name() + " " + roomName + " " + status;
        pw.println(request);
        pw.flush();
    }
}
