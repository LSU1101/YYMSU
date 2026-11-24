// 2271424 이상언
package client;

import common.msg.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Vector;

public class BrainBoardClient {
    private JFrame frame;

    // 상단 바
    private JLabel phaseLabel;
    private JLabel timeLabel;

    // 가운데 칸 보드
    private JPanel boardPanel; // 칸들을 가로로 붙이는 패널
    private Vector<GroupColumn> columns = new Vector<GroupColumn>(); // 0번은 항상 미분류
    private HashMap<String, CardView> cardViews = new HashMap<String, CardView>(); // cardId → 카드
    private CardView dragCard; // 지금 끌고 있는 카드

    // 오른쪽: 접속자, 최근 활동, 채팅
    private JTextArea userList;
    private JTextArea activity;
    private JTextArea display;
    private JTextField chatInput;
    private JButton send;

    // 아래쪽: 카드·칸 입력
    private JTextField cardInput;
    private JButton addCard;
    private JTextField groupInput;
    private JButton addGroup;

    // 아래쪽: 접속 정보와 버튼
    private JTextField userID;
    private JButton connect;
    private JButton disconnect;
    private JButton exit;

    private boolean connected = false; // 서버에 연결됨
    private boolean joined = false; // 입장까지 성공함
    private String myName;
    private boolean host = false;
    private String serverAddress;
    private int serverPort;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread receiveThread;

    public BrainBoardClient() {
        frame = new JFrame("BrainBoard");

        readServerInfo();
        buildGUI();

        frame.setBounds(150, 80, 1100, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    // server.txt에서 접속할 서버 주소와 포트 읽기 (1줄: 주소, 2줄: 포트)
    private void readServerInfo() {
        serverAddress = "localhost";
        serverPort = 54321;

        try {
            Scanner reader = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream("server.txt"), "UTF-8")));
            serverAddress = reader.next();
            serverPort = reader.nextInt();
            reader.close();
        } catch (IOException e) {
            System.out.println("server.txt를 읽을 수 없어 기본값을 사용합니다.");
        } catch (NoSuchElementException e) {
            System.out.println("server.txt 형식이 잘못되어 기본값을 사용합니다.");
        }
    }

    private void buildGUI() {
        frame.add(createTopPanel(), BorderLayout.NORTH);
        frame.add(createBoardPanel(), BorderLayout.CENTER);
        frame.add(createSidePanel(), BorderLayout.EAST);
        frame.add(createBottomPanel(), BorderLayout.SOUTH);

        updateButtonState();
    }

    // 상단 바: 프로그램 이름, 진행 단계, 남은 시간 (단계·시간은 최종 발표 전에 연결)
    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());

        JLabel title = new JLabel("  브레인보드");
        title.setFont(new Font("Dialog", Font.BOLD, 18));

        JPanel statePanel = new JPanel();
        statePanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        phaseLabel = new JLabel("단계: 쓰기");
        timeLabel = new JLabel("남은 시간  --:--");
        statePanel.add(phaseLabel);
        statePanel.add(new JLabel("   "));
        statePanel.add(timeLabel);

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(statePanel, BorderLayout.EAST);

        return topPanel;
    }

    // 가운데: 칸 보드, 칸이 많아지면 스크롤
    private JScrollPane createBoardPanel() {
        boardPanel = new JPanel();
        boardPanel.setLayout(new GridLayout(1, 0, 8, 0));

        // 칸들을 왼쪽에 붙이고 높이는 꽉 채우기
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BorderLayout());
        wrapper.add(boardPanel, BorderLayout.WEST);

        resetBoard();

        return new JScrollPane(wrapper);
    }

    // 보드를 비우고 미분류 칸만 남기기 (접속할 때마다 서버가 칸·카드를 다시 보내줌)
    private void resetBoard() {
        boardPanel.removeAll();
        columns.clear();
        cardViews.clear();
        dragCard = null;

        GroupColumn unsorted = new GroupColumn(null, "미분류");
        columns.add(unsorted);
        boardPanel.add(unsorted);

        boardPanel.revalidate();
        boardPanel.repaint();
    }

    // 오른쪽: 위에 접속자와 최근 활동, 아래에 채팅
    private JPanel createSidePanel() {
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new GridLayout(3, 1));
        sidePanel.setPreferredSize(new Dimension(300, 0));

        userList = new JTextArea();
        userList.setEditable(false);
        activity = new JTextArea();
        activity.setEditable(false);
        activity.setLineWrap(true);

        sidePanel.add(createTitledPanel("접속자", new JScrollPane(userList)));
        sidePanel.add(createTitledPanel("최근 활동", new JScrollPane(activity)));
        sidePanel.add(createChatPanel());

        return sidePanel;
    }

    // 제목 라벨 + 내용 하나로 된 패널
    private JPanel createTitledPanel(String title, JComponent content) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        panel.add(new JLabel(" " + title), BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createChatPanel() {
        JPanel chatPanel = new JPanel();
        chatPanel.setLayout(new BorderLayout());

        display = new JTextArea();
        display.setEditable(false);
        display.setLineWrap(true);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        chatInput = new JTextField();
        send = new JButton("보내기");
        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(send, BorderLayout.EAST);

        chatInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendChat();
            }
        });

        send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendChat();
            }
        });

        chatPanel.add(new JLabel(" 채팅"), BorderLayout.NORTH);
        chatPanel.add(new JScrollPane(display), BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        return chatPanel;
    }

    // 카드 등록, 칸 추가 입력
    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        cardInput = new JTextField(25);
        addCard = new JButton("카드 등록");
        groupInput = new JTextField(12);
        addGroup = new JButton("칸 추가");

        ActionListener cardListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendCard();
            }
        };
        cardInput.addActionListener(cardListener);
        addCard.addActionListener(cardListener);

        ActionListener groupListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendGroup();
            }
        };
        groupInput.addActionListener(groupListener);
        addGroup.addActionListener(groupListener);

        inputPanel.add(new JLabel("아이디어: "));
        inputPanel.add(cardInput);
        inputPanel.add(addCard);
        inputPanel.add(new JLabel("      새 칸 이름: "));
        inputPanel.add(groupInput);
        inputPanel.add(addGroup);

        return inputPanel;
    }

    // 아이디 입력과 접속할 서버 정보 (서버 정보는 server.txt에서 읽은 값)
    private JPanel createInfoPanel() {
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        userID = new JTextField(10);

        try {
            InetAddress local = InetAddress.getLocalHost();
            String addr = local.getHostAddress();
            String[] part = addr.split("\\.");
            userID.setText("guest" + part[3]);
        } catch (UnknownHostException e) {
            userID.setText("guest");
        }

        infoPanel.add(new JLabel("이름: "));
        infoPanel.add(userID);
        infoPanel.add(new JLabel("   서버: " + serverAddress + " : " + serverPort + " (server.txt)"));

        return infoPanel;
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(1, 3));

        connect = new JButton("접속하기");
        disconnect = new JButton("접속 끊기");
        exit = new JButton("종료");

        // 연결 후 입장 요청까지 보내기, 입장 결과는 receiveMessage()에서 처리
        connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connected = connectToServer();
                if (connected) {
                    sendMessage(new JoinRequest(userID.getText(), null));
                }
                updateButtonState();
            }
        });

        disconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                disconnect();
            }
        });

        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (connected) {
                    disconnect();
                }
                System.exit(0);
            }
        });

        controlPanel.add(connect);
        controlPanel.add(disconnect);
        controlPanel.add(exit);

        return controlPanel;
    }

    private JPanel createBottomPanel() { // 입력 패널, 정보 패널, 컨트롤 패널 합치기
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());

        bottomPanel.add(createInputPanel(), BorderLayout.NORTH);
        bottomPanel.add(createInfoPanel(), BorderLayout.CENTER);
        bottomPanel.add(createControlPanel(), BorderLayout.SOUTH);

        return bottomPanel;
    }

    private void updateButtonState() { // 버튼의 상태 관리
        connect.setEnabled(!connected);
        disconnect.setEnabled(connected);
        exit.setEnabled(!connected);
        userID.setEditable(!connected);

        // 입장까지 성공해야 채팅·카드·칸 입력 가능
        chatInput.setEnabled(joined);
        send.setEnabled(joined);
        cardInput.setEnabled(joined);
        addCard.setEnabled(joined);
        groupInput.setEnabled(joined);
        addGroup.setEnabled(joined);
    }

    private boolean connectToServer() {
        try {
            socket = new Socket(serverAddress, serverPort);

            // 출력 스트림을 먼저 만들고 flush (서버와 같은 순서)
            out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            out.flush();
            in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

            receiveThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (receiveThread == Thread.currentThread()) {
                        receiveMessage();
                    }
                }
            });
            receiveThread.start();

            return true;
        } catch (IOException e) {
            printDisplay("클라이언트 접속 오류> " + e.getMessage());
            return false;
        }
    }

    private void disconnect() {
        try {
            receiveThread = null;
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            printDisplay("클라이언트 종료 오류> " + e.getMessage());
        } finally {
            connected = false;
            joined = false;
            host = false;
            frame.setTitle("BrainBoard");
            userList.setText("");
            resetBoard();
            updateButtonState();
        }
    }

    private void sendMessage(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
            out.reset();
        } catch (IOException e) {
            printDisplay("메시지 보내기 오류> " + e.getMessage());
            disconnect();
        }
    }

    // 채팅 보내기, 화면에는 서버가 돌려준 내용만 출력
    private void sendChat() {
        String text = chatInput.getText();
        if (text.trim().isEmpty()) {
            return;
        }
        sendMessage(new ChatMessage(null, text));
        chatInput.setText("");
    }

    // 카드 등록 요청 (cardId는 서버가 발급)
    private void sendCard() {
        String text = cardInput.getText();
        if (text.trim().isEmpty()) {
            return;
        }
        sendMessage(new IdeaCard(text));
        cardInput.setText("");
    }

    // 칸 추가 요청 (groupId는 서버가 발급)
    private void sendGroup() {
        String name = groupInput.getText();
        if (name.trim().isEmpty()) {
            return;
        }
        sendMessage(new Group(name));
        groupInput.setText("");
    }

    // 메시지 하나를 받아 종류별로 처리
    private void receiveMessage() {
        try {
            Message msg = (Message) in.readObject();

            if (msg instanceof JoinResult) {
                handleJoinResult((JoinResult) msg);
            } else if (msg instanceof ChatMessage) {
                ChatMessage chat = (ChatMessage) msg;
                if ("SERVER".equals(chat.senderName)) {
                    printDisplay("*** " + chat.text + " ***");
                } else {
                    printDisplay(chat.senderName + ": " + chat.text);
                }
            } else if (msg instanceof UserListUpdate) {
                showUserList((UserListUpdate) msg);
            } else if (msg instanceof ActivityLog) {
                printActivity(((ActivityLog) msg).text);
            } else if (msg instanceof ErrorMessage) {
                printDisplay("[알림] " + ((ErrorMessage) msg).text);
            } else if (msg instanceof Group) {
                showGroup((Group) msg);
            } else if (msg instanceof IdeaCard) {
                showCard((IdeaCard) msg);
            } else if (msg instanceof CardMove) {
                moveCard((CardMove) msg);
            } else if (msg instanceof LockResult) {
                handleLockResult((LockResult) msg);
            }
        } catch (IOException e) {
            if (receiveThread != null) {
                printDisplay("서버 연결이 끊어졌습니다.");
                disconnect();
            }
        } catch (ClassNotFoundException e) {
            printDisplay("객체 읽기 오류> " + e.getMessage());
        }
    }

    // [3][4] 입장 결과
    private void handleJoinResult(JoinResult result) {
        if (result.ok) {
            joined = true;
            myName = userID.getText().trim();
            host = result.host;
            updateTitle();
            printDisplay("입장했습니다.");
            updateButtonState();
        } else {
            // 서버가 연결을 끊으므로 이쪽도 정리하고, 이름을 바꿔 다시 접속하게 함
            printDisplay("입장 실패: " + result.reason);
            disconnect();
        }
    }

    // [7] 접속자 목록 표시
    private void showUserList(UserListUpdate update) {
        userList.setText("");

        for (int i = 0; i < update.users.size(); i++) {
            UserInfo u = update.users.get(i);
            String line = u.name;
            if (u.host) {
                line += " (방장)";
            }
            if (u.name.equals(myName)) {
                line += " - 나";
                host = u.host; // 방장이 바뀌었을 수 있음
            }
            userList.append(line + "\n");
        }
        updateTitle();
    }

    // [12] 새 칸을 맨 오른쪽에 추가
    private void showGroup(Group group) {
        if (findColumn(group.groupId) != null) {
            return; // 이미 있는 칸
        }

        GroupColumn column = new GroupColumn(group.groupId, group.name);
        columns.add(column);
        boardPanel.add(column);
        boardPanel.revalidate();
        boardPanel.repaint();
    }

    // [11] 서버가 보낸 카드: 처음 보는 카드면 칸 맨 끝에 추가, 있던 카드면 모양만 갱신
    private void showCard(IdeaCard card) {
        CardView view = cardViews.get(card.cardId);

        if (view == null) {
            view = createCardView(card);
            cardViews.put(card.cardId, view);

            GroupColumn column = findColumn(card.groupId);
            if (column == null) {
                column = columns.get(0); // 모르는 칸이면 미분류에
            }
            column.addCard(view, -1);
        } else {
            view.update(card, myName);
        }
    }

    // [15] 카드를 원래 칸에서 빼서 새 칸의 toIndex번째에 넣기
    private void moveCard(CardMove move) {
        CardView view = cardViews.get(move.cardId);
        GroupColumn from = findColumn(move.fromGroupId);
        GroupColumn to = findColumn(move.toGroupId);
        if (view == null || from == null || to == null) {
            return;
        }

        from.removeCard(view);
        to.addCard(view, move.toIndex);
        view.card.groupId = move.toGroupId;
    }

    // [14] 잠금 실패면 끌기 취소
    private void handleLockResult(LockResult result) {
        if (!result.granted) {
            printDisplay("[알림] " + result.holderName + "님이 옮기는 중인 카드입니다.");
            if (dragCard != null && dragCard.card.cardId.equals(result.cardId)) {
                dragCard.setCursor(Cursor.getDefaultCursor());
                dragCard = null;
            }
        }
    }

    // groupId로 칸 찾기 (null = 미분류)
    private GroupColumn findColumn(String groupId) {
        for (int i = 0; i < columns.size(); i++) {
            GroupColumn c = columns.get(i);
            if (groupId == null) {
                if (c.groupId == null) {
                    return c;
                }
            } else if (groupId.equals(c.groupId)) {
                return c;
            }
        }
        return null;
    }

    // 카드를 만들고 마우스로 끌 수 있게 연결
    private CardView createCardView(IdeaCard card) {
        final CardView view = new CardView(card, myName);

        view.addMouseListener(new MouseListener() {
            @Override
            public void mousePressed(MouseEvent e) { // 누르면 잠금 요청
                startDrag(view);
            }

            @Override
            public void mouseReleased(MouseEvent e) { // 놓으면 이동 요청
                endDrag(view, e.getXOnScreen(), e.getYOnScreen());
            }

            @Override
            public void mouseClicked(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        return view;
    }

    // [13] 카드를 누르면 서버에 잠금 요청
    private void startDrag(CardView view) {
        if (!joined) {
            return;
        }
        if (view.isLockedByOther(myName)) {
            printDisplay("[알림] " + view.card.lockedBy + "님이 옮기는 중인 카드입니다.");
            return;
        }

        dragCard = view;
        view.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        sendMessage(new LockRequest(view.card.cardId, "LOCK"));
    }

    // [15] 놓은 위치의 칸과 순서를 찾아 이동 요청, 제자리면 잠금 해제
    private void endDrag(CardView view, int screenX, int screenY) {
        if (dragCard != view) {
            return; // 잠금 실패 등으로 끌기가 취소됨
        }
        dragCard = null;
        view.setCursor(Cursor.getDefaultCursor());

        GroupColumn from = findColumn(view.card.groupId);
        GroupColumn to = null;
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).containsScreenX(screenX)) {
                to = columns.get(i);
                break;
            }
        }

        if (to == null || from == null) {
            sendMessage(new LockRequest(view.card.cardId, "UNLOCK")); // 칸 밖에 놓음
            return;
        }

        int toIndex = to.dropIndex(screenY, view);
        if (to == from && toIndex == from.indexOf(view)) {
            sendMessage(new LockRequest(view.card.cardId, "UNLOCK")); // 제자리
            return;
        }

        // 화면은 서버가 돌려준 CardMove를 받고 나서 바뀜
        sendMessage(new CardMove(view.card.cardId, from.groupId, to.groupId, toIndex));
    }

    private void updateTitle() {
        if (host) {
            frame.setTitle("BrainBoard - " + myName + " (방장)");
        } else {
            frame.setTitle("BrainBoard - " + myName);
        }
    }

    private void printDisplay(String msg) {
        display.append(msg + "\n");
        display.setCaretPosition(display.getDocument().getLength());
    }

    private void printActivity(String msg) {
        activity.append(msg + "\n");
        activity.setCaretPosition(activity.getDocument().getLength());
    }

    public static void main(String[] args) {
        new BrainBoardClient();
    }
}
