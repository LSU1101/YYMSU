// 2271424 이상언
package common.msg;

// [10] 카드 등록 요청 (C -> S), cardId는 null로 보냄
// [11] 카드 전체 전송 (S -> C), 화면은 이 메시지를 받아야 카드 추가
public class IdeaCard extends Message {
    private static final long serialVersionUID = 1L;

    public String cardId; // 서버가 발급
    public String text;
    public String authorName;
    public String groupId; // null이면 미분류 칸
    public int order;
    public String lockedBy; // 잡고 있는 사람, null이면 잠금 없음

    public IdeaCard(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "[카드] " + cardId + " " + text + " (작성: " + authorName + ", 칸: " + groupId + ", 잠금: " + lockedBy + ")";
    }
}
