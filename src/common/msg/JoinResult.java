// 2271424 이상언
package common.msg;

// [3][4] 입장 결과 (S -> C)
public class JoinResult extends Message {
    private static final long serialVersionUID = 1L;

    public boolean ok;
    public String userId;
    public String sessionToken;
    public boolean host; // 방장 여부
    public String reason; // 실패 사유

    public JoinResult(boolean ok, String userId, String sessionToken, boolean host, String reason) {
        this.ok = ok;
        this.userId = userId;
        this.sessionToken = sessionToken;
        this.host = host;
        this.reason = reason;
    }

    @Override
    public String toString() {
        if (ok) {
            return "[입장성공] " + userId + " 방장: " + host;
        }
        return "[입장실패] " + reason;
    }
}
