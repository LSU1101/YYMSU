// 2271424 이상언
package common.msg;

import java.io.Serializable;

// 모든 메시지의 공통 부모
public abstract class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public String senderId; // 보낸 사람 (서버가 덮어씀)
    public long timestamp; // 만든 시각
    public int requestId; // 요청과 응답 짝짓기용

    public Message() {
        timestamp = System.currentTimeMillis();
    }
}
