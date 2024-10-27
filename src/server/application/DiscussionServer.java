package server.application;

import server.controller.ChatThread;
import server.domain.Room;
import server.domain.User;

import java.net.*;
import java.util.*;

public class DiscussionServer {
    public static void main(String[] args) {
        try {
            ServerSocket server = new ServerSocket(10001);
            System.out.println("접속을 기다립니다.");
            HashMap<User, Socket> userMap = new HashMap<>();
            HashMap<Room, HashMap> roomMap = new HashMap<>();

            //메인스레드는 이 동작만 무한 루프를 돌린다
            while (true) {
                //클라이언트가 요청하기 전까지 Block
                //accept 상태에서 요청을 받는다면 Socket을 반환
                Socket sock = server.accept();
                ChatThread chatthread = new ChatThread(sock, roomMap, userMap);
                chatthread.start();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}