// 2271424 이상언
package common.msg;

// [13] 카드 잠금 요청 (C -> S), 드래그 시작할 때 보냄
public class LockRequest extends Message {
    private static final long serialVersionUID = 1L;

    public String cardId;
    public String action; // "LOCK" 또는 "UNLOCK"

    public LockRequest(String cardId, String action) {
        this.cardId = cardId;
        this.action = action;
    }

    @Override
    public String toString() {
        return "[잠금요청] " + cardId + " " + action;
    }
}
