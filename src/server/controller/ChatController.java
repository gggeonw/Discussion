package server.controller;

import server.domain.Chat;
import server.domain.Room;
import server.domain.Status;
import server.domain.User;
import server.repository.ChatRepository;
import server.repository.RoomRepository;

import java.io.PrintWriter;
import java.util.*;

public class ChatController {
    private RoomRepository roomRepository = new RoomRepository();
    private ChatRepository chatRepository = new ChatRepository();
    private RoomController roomController;
    private Map<String, List<PrintWriter>> userMap;

    public ChatController(Map<String, List<PrintWriter>> userMap) {
        this.userMap = userMap;
    }

    public void createRoom(Room room){
        chatRepository.createChatFile(room);
        roomRepository.createRoom(room);
        System.out.println(room.getRoomName() + " 생성 완료");
    }

    public void printRoomList(User user){
        ArrayList<Room> rooms = roomRepository.getRoomList();
        synchronized (user) {
            System.out.println("printRoomList called with user: " + user);
            if (user == null) {
                throw new IllegalArgumentException("User is null.");
            }
            PrintWriter pw = user.getPrintWriter();
            System.out.println("PrintWriter: " + pw);
            System.out.println("Room List: " + rooms);
            pw.println("------------- 토론 채팅방 리스트 -------------");
            pw.flush();
            for (Room room : rooms) {
                pw.println(room.toString());
            }
            pw.println("----------------------------------------------");
            pw.flush();
        }
    }


    // 채팅방 입장
    public Room enterRoom(String roomName, User user){
        Room room = roomRepository.findRoomByName(roomName);
        if (room == null) {
            throw new IllegalArgumentException("Room not found: " + roomName);
        }
        room = roomRepository.addUserToRoom(roomName, user); // addUserToRoom 호출
        sendToClient(user,user.getUserName() + " 님이 " + roomName + " 토론 채팅방에 입장하였습니다.");
        ArrayList<Chat> chats = chatRepository.findChatHistory(room);
        synchronized (user) {
            PrintWriter pw = user.getPrintWriter();
            for (Chat chat : chats) {
                String message = chat.getMessage();
                pw.println(message);
                pw.flush();
            }
        }
        return room;
    }

    // 채팅방 Status 선택
    public void selectStatus(String roomName, User user){
        Room room = roomRepository.findRoomByName(roomName);
        String firstStatus = room.getFirstStatus();
        String secondStatus = room.getSecondStatus();

        synchronized (user){
            PrintWriter printWriter = user.getPrintWriter();
            printWriter.println("Status 선택");
            printWriter.println(firstStatus + "\t\t\t" + "NONE" + "\t\t\t" + secondStatus);
            printWriter.flush();
        }
    }

    // 채팅방 퇴장
    public void exitRoom(Room room,User user){
        room.removeUser(user);
        System.out.println(user.getUserName() + " 님이 " + room.getRoomName() + " 에서 퇴장하였습니다.");
    }

    //상태에 따른 구분 필요
    public void chat(Room room, User user, HashMap userMap, String status, String message){
        // 디버깅 출력
        System.out.println("Room: " + room);
        System.out.println("User: " + user);
        System.out.println("User Map: " + userMap);
        System.out.println("User Status: " + status);
        System.out.println("Message: " + message);
        if (room == null) {
            throw new IllegalArgumentException("Room is null.");
        }
        List<PrintWriter> userList = (List<PrintWriter>) userMap.get(room.getRoomName());
        if (userList == null) {
            userList = new ArrayList<>(); // 빈 리스트로 초기화
            userMap.put(room.getRoomName(), userList); // userMap에 추가
            System.out.println("User list was null. Initialized and added to userMap.");
        }

        System.out.println("Room User List: " + userList);
        String selectStatus = "";
        if (status.equals(room.getFirstStatus())){
            selectStatus = room.getFirstStatus();
        }
        if (status.equals(room.getSecondStatus())){
            selectStatus = room.getSecondStatus();
        }

        String chat = selectStatus + " : " + message;
//        synchronized (userList){
//            Collection<PrintWriter> collection = new ArrayList<>();
//
//            Iterator iter = collection.iterator();
//            while (iter.hasNext()) {
//                PrintWriter pw = (PrintWriter) iter.next();
//                pw.println(chat);
//                pw.flush();
//            }
//        }
        synchronized (userList) {
            for (PrintWriter pw : userList) {
                pw.println(chat); // 메시지 출력
                pw.flush();       // 버퍼 비우기
            }
        }
        String userName = user.getUserName();
        Chat chatHistory = new Chat(userName, chat, status);
        chatRepository.saveChat(room, chatHistory);
    }

    public void sendToClient(User user, String msg){
        synchronized (user) {
            PrintWriter pw = user.getPrintWriter();
            pw.println(msg);
            pw.flush();
        }
    }

    public void deleteExpiredRooms() {
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.deleteExpiredRooms(); // 만료된 방 삭제
    }

}