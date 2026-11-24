// 2271424 이상언
package common.msg;

// [23] 요청 실패 응답 (S -> C), 보낸 사람에게만
// code: "NOT_JOINED", "BAD_REQUEST", "LOCK_DENIED", "WRONG_PHASE",
//       "NOT_HOST", "DUPLICATE_VOTE", "BANNED_WORD"
public class ErrorMessage extends Message {
    private static final long serialVersionUID = 1L;

    public String code;
    public String text;

    public ErrorMessage(String code, String text) {
        this.code = code;
        this.text = text;
    }

    @Override
    public String toString() {
        return "[오류] " + code + " " + text;
    }
}
