// 2271424 이상언
package client;

import common.msg.IdeaCard;

import javax.swing.*;
import java.awt.*;

// 보드 위의 카드 하나 (내용, 작성자, 잠금 상태 표시)
public class CardView extends JPanel {
    IdeaCard card; // 서버가 보낸 카드 정보
    private JLabel textLabel;
    private JLabel infoLabel;

    public CardView(IdeaCard card, String myName) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(180, 50));

        textLabel = new JLabel();
        textLabel.setFont(new Font("Dialog", Font.BOLD, 13));
        infoLabel = new JLabel();
        infoLabel.setFont(new Font("Dialog", Font.PLAIN, 11));

        add(textLabel, BorderLayout.CENTER);
        add(infoLabel, BorderLayout.SOUTH);

        update(card, myName);
    }

    // 새 카드 정보로 화면 갱신
    public void update(IdeaCard card, String myName) {
        this.card = card;
        textLabel.setText(" " + card.text);

        if (card.lockedBy == null) {
            // 아무도 안 잡음
            setBackground(Color.WHITE);
            infoLabel.setForeground(Color.GRAY);
            infoLabel.setText(" " + card.authorName);
        } else if (card.lockedBy.equals(myName)) {
            // 내가 잡는 중
            setBackground(new Color(255, 240, 180));
            infoLabel.setForeground(Color.DARK_GRAY);
            infoLabel.setText(" 내가 옮기는 중");
        } else {
            // 다른 사람이 잡는 중 → 회색, 마우스로 못 잡음
            setBackground(new Color(200, 200, 200));
            infoLabel.setForeground(Color.RED);
            infoLabel.setText(" " + card.lockedBy + "님이 옮기는 중");
        }
        repaint();
    }

    // 다른 사람이 잡고 있는 카드인지
    public boolean isLockedByOther(String myName) {
        return card.lockedBy != null && !card.lockedBy.equals(myName);
    }
}
