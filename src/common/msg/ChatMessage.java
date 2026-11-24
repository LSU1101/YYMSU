// 2271424 이상언
package common.msg;

// [5][6][9] 채팅 (C -> S, S -> C)
// 입장·퇴장 안내는 senderName이 "SERVER"
public class ChatMessage extends Message {
    private static final long serialVersionUID = 1L;

    public String senderName; // 서버가 채움
    public String text;

    public ChatMessage(String senderName, String text) {
        this.senderName = senderName;
        this.text = text;
    }

    @Override
    public String toString() {
        return "[채팅] " + senderName + ": " + text;
    }
}
