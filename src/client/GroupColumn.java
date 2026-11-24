// 2271424 이상언
package client;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

// 보드의 칸 하나 (제목 + 카드를 위에서부터 한 줄씩)
public class GroupColumn extends JPanel {
    String groupId; // null이면 미분류 칸
    String name;
    private JLabel title;
    private JPanel cardPanel;
    private Vector<CardView> cards = new Vector<CardView>();

    public GroupColumn(String groupId, String name) {
        this.groupId = groupId;
        this.name = name;

        setLayout(new BorderLayout());
        setBackground(new Color(235, 235, 235));

        title = new JLabel();
        title.setFont(new Font("Dialog", Font.BOLD, 14));

        // 카드가 칸 높이만큼 늘어나지 않도록 NORTH에 붙임
        cardPanel = new JPanel();
        cardPanel.setLayout(new GridLayout(0, 1, 0, 6));
        cardPanel.setOpaque(false);
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(cardPanel, BorderLayout.NORTH);

        add(title, BorderLayout.NORTH);
        add(wrapper, BorderLayout.CENTER);

        refresh();
    }

    // index번째에 카드 넣기 (범위를 넘으면 맨 끝)
    public void addCard(CardView view, int index) {
        if (index < 0 || index > cards.size()) {
            index = cards.size();
        }
        cards.add(index, view);
        refresh();
    }

    public void removeCard(CardView view) {
        cards.removeElement(view);
        refresh();
    }

    public int indexOf(CardView view) {
        return cards.indexOf(view);
    }

    // 화면 좌표 x가 이 칸 안에 있는지
    public boolean containsScreenX(int screenX) {
        Point p = getLocationOnScreen();
        return screenX >= p.x && screenX < p.x + getWidth();
    }

    // 화면 좌표 y에 놓으면 몇 번째 자리인지 (끌고 있는 카드는 빼고 셈)
    public int dropIndex(int screenY, CardView dragging) {
        int index = 0;

        for (int i = 0; i < cards.size(); i++) {
            CardView v = cards.get(i);
            if (v == dragging) {
                continue;
            }
            int centerY = v.getLocationOnScreen().y + v.getHeight() / 2;
            if (centerY < screenY) {
                index++;
            }
        }
        return index;
    }

    // 카드 목록대로 다시 붙이고 제목의 카드 수 갱신
    private void refresh() {
        cardPanel.removeAll();
        for (int i = 0; i < cards.size(); i++) {
            cardPanel.add(cards.get(i));
        }
        title.setText(" " + name + "  (" + cards.size() + ")");

        revalidate();
        repaint();
    }
}
