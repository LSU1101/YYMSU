// 2271424 이상언
package common.msg;

// [15] 카드 이동 (C -> S, S -> C), groupId가 null이면 미분류 칸
public class CardMove extends Message {
    private static final long serialVersionUID = 1L;

    public String cardId;
    public String fromGroupId;
    public String toGroupId;
    public int toIndex; // 옮길 칸 안에서의 순서

    public CardMove(String cardId, String fromGroupId, String toGroupId, int toIndex) {
        this.cardId = cardId;
        this.fromGroupId = fromGroupId;
        this.toGroupId = toGroupId;
        this.toIndex = toIndex;
    }

    @Override
    public String toString() {
        return "[이동] " + cardId + " " + fromGroupId + " -> " + toGroupId + " (" + toIndex + "번째)";
    }
}
