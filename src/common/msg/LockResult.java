// 2271424 이상언
package common.msg;

// [14] 잠금 요청 결과 (S -> C), 실패면 holderName이 잡고 있는 사람
public class LockResult extends Message {
    private static final long serialVersionUID = 1L;

    public String cardId;
    public boolean granted; // 잠금 성공 여부
    public String holderName;

    public LockResult(String cardId, boolean granted, String holderName) {
        this.cardId = cardId;
        this.granted = granted;
        this.holderName = holderName;
    }

    @Override
    public String toString() {
        return "[잠금결과] " + cardId + " 성공: " + granted + " (잡은 사람: " + holderName + ")";
    }
}
