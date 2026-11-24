// 2271424 이상언
package server;

import common.msg.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.Vector;

public class BrainBoardServer {
    private JFrame frame;
    private JButton connect;
    private JButton disconnect;
    private JButton exit;
    private JTextArea display;
    private int port;
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private Vector<ClientHandler> users = new Vector<ClientHandler>();
    private BoardManager board = new BoardManager();
    private int userCount = 0; // userId 발급용 번호

    public BrainBoardServer(int port) {
        frame = new JFrame("BrainBoard Server");
        this.port = port;

        buildGUI();

        frame.setBounds(200, 100, 700, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private void buildGUI() {
        frame.add(createDisplayPanel(), BorderLayout.CENTER);
        frame.add(createControlPanel(), BorderLayout.SOUTH);
    }

    private JPanel createDisplayPanel() {
        JPanel displayPanel = new JPanel();
        displayPanel.setLayout(new BorderLayout());

        display = new JTextArea();
        display.setEditable(false);
        displayPanel.add(new JScrollPane(display), BorderLayout.CENTER);

        return displayPanel;
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(1, 3));

        connect = new JButton("서버 시작");
        disconnect = new JButton("서버 종료");
        exit = new JButton("종료");

        connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                acceptThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        startServer();
                    }
                });
                acceptThread.start();

                connect.setEnabled(false);
                disconnect.setEnabled(true);
                exit.setEnabled(false);
            }
        });

        disconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                disconnect();

                connect.setEnabled(true);
                disconnect.setEnabled(false);
                exit.setEnabled(true);
            }
        });

        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                disconnect();
                System.exit(0);
            }
        });

        disconnect.setEnabled(false);

        controlPanel.add(connect);
        controlPanel.add(disconnect);
        controlPanel.add(exit);

        return controlPanel;
    }

    private String getLocalAddr() {
        try {
            InetAddress local = InetAddress.getLocalHost();
            return local.getHostAddress();
        } catch (UnknownHostException e) {
            return "알 수 없음";
        }
    }

    private void startServer() {
        Socket clientSocket = null;

        try {
            serverSocket = new ServerSocket(port);
            printDisplay("서버가 시작되었습니다: " + getLocalAddr() + " (포트 " + port + ")");

            while (acceptThread == Thread.currentThread()) {
                clientSocket = serverSocket.accept();
                String cAddr = clientSocket.getInetAddress().getHostAddress();
                printDisplay("클라이언트가 연결되었습니다: " + cAddr);

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                users.add(clientHandler);
                clientHandler.start();
            }
        } catch (SocketException e) {
            printDisplay("서버 소켓 종료");
        } catch (IOException e) {
            printDisplay("서버 오류> " + e.getMessage());
        }
    }

    private void disconnect() {
        try {
            acceptThread = null;
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            printDisplay("서버 소켓 닫기 오류> " + e.getMessage());
        }

        // 접속 중인 클라이언트 연결도 모두 닫기
        // 닫는 동안 users에서 빠져나가므로 복사본으로 반복
        Vector<ClientHandler> list = new Vector<ClientHandler>(users);
        for (int i = 0; i < list.size(); i++) {
            list.get(i).closeSocket();
        }
    }

    private void printDisplay(String msg) {
        display.append(msg + "\n");
        display.setCaretPosition(display.getDocument().getLength());
    }

    // 입장을 마친 사람들의 목록 만들기
    private UserListUpdate makeUserList() {
        Vector<UserInfo> list = new Vector<UserInfo>();

        for (int i = 0; i < users.size(); i++) {
            ClientHandler c = users.get(i);
            if (c.userName != null) {
                list.add(new UserInfo(c.userName, c.host, "ONLINE"));
            }
        }
        return new UserListUpdate(list);
    }

    // 입장을 마친 사람 수
    private int joinedCount() {
        int count = 0;

        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).userName != null) {
                count++;
            }
        }
        return count;
    }

    // 이미 쓰고 있는 이름인지
    private boolean isNameUsed(String name) {
        for (int i = 0; i < users.size(); i++) {
            if (name.equals(users.get(i).userName)) {
                return true;
            }
        }
        return false;
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String userId; // 입장 전에는 null
        private String userName; // 입장 전에는 null
        private boolean host = false;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        private void receiveMessages(Socket cs) {
            try {
                // 출력 스트림을 먼저 만들고 flush (양쪽이 입력 스트림부터 만들면 서로 기다리며 멈춤)
                out = new ObjectOutputStream(new BufferedOutputStream(cs.getOutputStream()));
                out.flush();
                in = new ObjectInputStream(new BufferedInputStream(cs.getInputStream()));

                while (true) {
                    Message msg = (Message) in.readObject();
                    printDisplay("수신 < " + who() + ": " + msg);

                    // 입장 전에는 JoinRequest만 받기
                    if (userName == null) {
                        if (msg instanceof JoinRequest) {
                            boolean ok = handleJoin((JoinRequest) msg);
                            if (!ok) {
                                break; // 입장 실패면 연결 종료
                            }
                        } else {
                            sendMessage(new ErrorMessage("NOT_JOINED", "먼저 입장해야 합니다."));
                        }
                        continue;
                    }

                    msg.senderId = userId; // 보낸 사람은 서버가 덮어씀

                    if (msg instanceof ChatMessage) {
                        handleChat((ChatMessage) msg);
                    } else {
                        sendMessage(new ErrorMessage("BAD_REQUEST", "아직 처리하지 않는 요청입니다."));
                    }
                }
            } catch (IOException e) {
                printDisplay(who() + " 연결 종료");
            } catch (ClassNotFoundException e) {
                printDisplay("객체 읽기 오류> " + e.getMessage());
            } finally {
                removeUser();
                closeSocket();
            }
        }

        // [2] 입장 요청 처리, 성공하면 true
        private boolean handleJoin(JoinRequest req) {
            String name = req.userName;
            if (name != null) {
                name = name.trim();
            }

            if (name == null || name.length() == 0 || name.length() > 20) {
                sendMessage(new JoinResult(false, null, null, false, "이름은 1~20자로 입력하세요."));
                return false;
            }
            if (isNameUsed(name)) {
                sendMessage(new JoinResult(false, null, null, false, "이미 사용 중인 이름입니다."));
                return false;
            }

            userCount++;
            userId = "U" + userCount;
            host = (joinedCount() == 0); // 첫 입장자가 방장
            userName = name;

            // [3] 본인에게 입장 성공 (sessionToken은 재접속 기능에서 사용 예정)
            sendMessage(new JoinResult(true, userId, null, host, null));

            // 늦게 들어온 사람에게 지금까지의 칸과 카드 보내기
            for (int i = 0; i < board.groups.size(); i++) {
                sendMessage(board.groups.get(i));
            }
            for (int i = 0; i < board.cards.size(); i++) {
                sendMessage(board.cards.get(i));
            }

            // [5][7] 전체에 입장 안내와 접속자 목록
            broadcasting(new ChatMessage("SERVER", userName + "님이 입장했습니다."));
            broadcasting(makeUserList());
            printDisplay("새 참가자: " + userName + " / 현재 참가자 수: " + joinedCount());
            return true;
        }

        // [9] 채팅: 보낸 사람 이름을 붙여 전체에 전달
        private void handleChat(ChatMessage msg) {
            if (msg.text == null || msg.text.trim().length() == 0) {
                sendMessage(new ErrorMessage("BAD_REQUEST", "빈 메시지는 보낼 수 없습니다."));
                return;
            }
            msg.senderName = userName;
            broadcasting(msg);
        }

        // [6][7] 퇴장 처리
        private void removeUser() {
            users.removeElement(this);

            if (userName == null) {
                return; // 입장 전에 나간 경우는 안내 없음
            }

            // 방장이 나가면 남은 사람 중 첫 번째가 방장
            if (host) {
                for (int i = 0; i < users.size(); i++) {
                    ClientHandler c = users.get(i);
                    if (c.userName != null) {
                        c.host = true;
                        broadcasting(new ActivityLog(c.userName + "님이 방장이 되었습니다."));
                        break;
                    }
                }
            }

            broadcasting(new ChatMessage("SERVER", userName + "님이 나갔습니다."));
            broadcasting(makeUserList());
            printDisplay(userName + " 퇴장. 현재 참가자 수: " + joinedCount());
        }

        // 로그에 표시할 이름 (입장 전에는 이름이 없음)
        private String who() {
            if (userName == null) {
                return "(입장 전)";
            }
            return userName;
        }

        private void closeSocket() {
            try {
                clientSocket.close();
            } catch (IOException e) {
                printDisplay("서버 닫기 오류> " + e.getMessage());
            }
        }

        // 실제로 스트림에 쓰는 부분
        // 여러 스레드가 같은 out에 동시에 쓰면 객체가 섞이므로 synchronized
        private synchronized void send(Message msg) {
            try {
                out.writeObject(msg);
                out.flush();
                out.reset(); // 같은 객체를 값만 바꿔 다시 보낼 때 새 값이 가도록
            } catch (IOException e) {
                printDisplay("클라이언트 전송 오류> " + e.getMessage());
            }
        }

        // 이 사람에게만 보내기
        private void sendMessage(Message msg) {
            printDisplay("송신 > " + who() + ": " + msg);
            send(msg);
        }

        // 입장한 모든 사람에게 보내기
        private void broadcasting(Message msg) {
            printDisplay("방송 > " + msg);
            for (int i = 0; i < users.size(); i++) {
                ClientHandler c = users.get(i);
                if (c.userName != null) {
                    c.send(msg);
                }
            }
        }

        @Override
        public void run() {
            receiveMessages(clientSocket);
        }
    }

    // 보드 상태 보관 (ScoreManager처럼 내부 클래스)
    // 카드 등록·이동·잠금 처리는 4단계에서 추가
    private class BoardManager {
        private Vector<Group> groups = new Vector<Group>();
        private Vector<IdeaCard> cards = new Vector<IdeaCard>();
    }

    public static void main(String[] args) {
        int port = 54321;

        new BrainBoardServer(port);
    }
}
