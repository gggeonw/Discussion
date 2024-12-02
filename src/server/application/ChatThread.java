package server.application;
//senttoclient삭제
import server.controller.ChatController;
import server.controller.RoomController;
import server.repository.ChatRepository;
import server.repository.RoomRepository;
import server.domain.Chat;
import server.domain.Room;
import server.domain.Status;
import server.domain.User;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;

public class ChatThread extends Thread {
    private Socket sock;
    private String userName;
    private BufferedReader br;  //buffer 사용 용도
    private PrintWriter pw;  //println() 사용 용도
    private User user;
    private Room room;
    private String status;
    private HashMap<String, List<PrintWriter>> userMap;

    private ChatRepository chatRepository;
    private RoomRepository roomRepository;
    private ChatController chatController;
    private boolean inRoom = false;
    private boolean hasStatus = false;
    private RoomController roomController;

    public ChatThread(Socket sock, HashMap userMap) {
        this.sock = sock;
        this.userMap = userMap;
        this.chatRepository = new ChatRepository();
        this.roomRepository = new RoomRepository();
        this.roomController = new RoomController(userMap);
        this.chatController = new ChatController(userMap);
        try {
            pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
            br = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            //클라이언트가 접속 되면 id를 전송한다는 프로토콜을 정의했기 때문에 readLine()을 통해 id를 받는다
            userName = br.readLine();
            user = new User(userName, pw);
            System.out.println("userName = " + userName);

            //chatController.printRoomList(user);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void run() {
        try {
            String line = null;
            while ((line = br.readLine()) != null) {
                if(line.split(" ")[0].equals("/c")){
                    String[] s = line.split(" ");
                    String roomName = s[1];
                    String firstStatus = s[2];
                    String secondStatus = s[3];

                    Room room = new Room(roomName, firstStatus, secondStatus, userName);
                    chatController.createRoom(room);
                    //chatController.sendToClient(user, user.getUserName() + " 님이 " + roomName + " 토론 채팅방을 생성하였습니다.");
                }

                if(line.split(" ")[0].equals("/exit") && inRoom) {
                    chatController.exitRoom(room, user);
                    roomController.removeUserFromRoom(room.getRoomName(), user.getPrintWriter());
                    //chatController.sendToClient(user, user.getUserName() + " 님이 " + room.getRoomName() + "방을 퇴장하였습니다.");
                    inRoom = false;
                }

                if(inRoom && hasStatus){
                    chatController.chat(room, user, userMap, status, line);
                }

                //Status 선택 메소드 추가 필요
                if(line.split(" ")[0].equals("/e")){
                    System.out.println("입장 명령 받음");
                    String[] s = line.split(" ");
                    String roomName = s[1];
                    String selectedStatus = s[2];
//                    chatController.selectStatus(roomName, user);
//                    String selectStatus = br.readLine();
//                    if (selectStatus.equals("1")){
//                        status = "찬성";
//                        this.hasStatus = true;
//                    }
//                    if (selectStatus.equals("2")){
//                        status = "반대";
//                        this.hasStatus = true;
//                    }
//                    if (selectStatus.equals("3")){
//                        status = "중립";
//                    }
                    System.out.println("입장 roomName 확인: " + roomName);
                    Room enteredRoom = chatController.enterRoom(roomName, user);
                    room = enteredRoom;
//                    String roomData = String.join(" ",
//                            enteredRoom.getRoomName(),
//                            enteredRoom.getUserName(),
//                            String.valueOf(enteredRoom.getFirstStatusCount()),
//                            String.valueOf(enteredRoom.getSecondStatusCount())
//                    );
//                    pw.println(roomData);
                    String enteredRoomName = enteredRoom.getRoomName();
                    System.out.println("입장 enteredRoomName 확인:" + enteredRoomName);
                    pw.println("ROOM:" + enteredRoomName);
                    pw.flush();
                    pw.println("ENTER_END"); // 응답 종료
                    pw.flush();
                    inRoom = true;
                    //room = enteredRoom;
                    System.out.println("chatThread, 입장, userMap.toString(): " + userMap.toString());
                }
                if (line.contains("/list")) {
                    List<Room> roomList = roomController.getRoomList();
                    for (Room room : roomList) {
                        String roomData = String.join(",",
                                room.getRoomName(),
                                room.getUserName(),
                                String.valueOf(room.getFirstStatusCount()),
                                String.valueOf(room.getSecondStatusCount()),
                                room.getFirstStatus(),
                                room.getSecondStatus()
                        );
                        System.out.println("roomData확인: " + roomData);
                        pw.println(roomData);
                        pw.flush();
                    }
                    pw.println("LIST_END"); // 응답 종료
                    pw.flush();
                }

                if (line.startsWith("/chat")) {
                    if (line.length() <= 6) { // "/chat " 이후 내용이 없는 경우
                        pw.println("Error: 메시지가 비어 있습니다.");
                        pw.flush();
                        continue;
                    }
                    String content = line.substring(6).trim(); // 메시지 내용 추출
                    System.out.println("content 확인: " + content);
                    int lastIndex = content.lastIndexOf(" ");
                    String message;
                    String status;
                    message = content.substring(0, lastIndex).trim(); // 마지막 단어 전까지를 메시지로 설정
                    status = content.substring(lastIndex + 1).trim();
                    Chat chat = new Chat(user.getUserName(), message, status);
                    if (status == null) status = "중립"; // 기본값 설정
                    chatRepository.saveChat(room, chat);
                    pw.println("CHAT_END"); // 응답 종료
                    pw.flush();
                }

                if (line.startsWith("/find")) {
                    String[] commandParts = line.split(" ");
                    if (commandParts.length < 2) {
                        pw.println("ERROR");
                        pw.flush();
                        continue;
                    }
                    String roomName = commandParts[1];
                    System.out.println("commandParts[1]을 roomName으로 받는지 확인: " + roomName);
                    Room room = roomRepository.findRoomByName(roomName);
                    System.out.println("room 확인: " + room);
                    if (room != null) {
                        String roomData = String.join(",",
                                room.getRoomName(),
                                room.getFirstStatus(),
                                room.getSecondStatus(),
                                room.getUserName(),
                                String.valueOf(room.getFirstStatusCount()),
                                String.valueOf(room.getSecondStatusCount())
                        );
                        pw.println(roomData); // 방 정보 전송
                        pw.flush();
                    } else {
                        pw.println("ERROR"); // 방을 찾을 수 없음
                        pw.flush();
                    }
                    pw.println("FIND_END"); // 응답 종료
                    pw.flush();
                }

                if (line.startsWith("/history")) {
                    String[] commandParts = line.split(" ");
                    if (commandParts.length < 2) {
                        pw.println("ERROR: 방 이름을 입력하세요.");
                        pw.flush();
                        continue;
                    }
                    String roomName = commandParts[1];
                    System.out.println("/chatHistory 요청, 방 이름: " + roomName);

                    // Room 객체를 통해 채팅 기록 가져오기
                    Room room = roomRepository.findRoomByName(roomName);
                    System.out.println("/chatHistory에서 room 확인: " + room);
                    if (room == null) {
                        pw.println("ERROR: 해당 방을 찾을 수 없습니다.");
                        pw.flush();
                        continue;
                    }

                    List<Chat> chatHistory = chatRepository.readChatHistory(room);
                    System.out.println("chathistory확인: " + chatHistory);
                    if (chatHistory == null || chatHistory.isEmpty()) {
                        pw.println("해당 방의 채팅 기록이 없습니다.");
                        pw.flush();
                        continue;
                    }

                    // 채팅 기록 클라이언트로 전송
                    pw.println(chatHistory);
                    pw.flush();

                    pw.println("HISTORY_END"); // 전송 완료 표시
                    pw.flush();
                }

            }
        } catch (Exception ex) {
            System.out.println(ex);
        } finally {
            try {
                if (sock != null)
                    sock.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }
}