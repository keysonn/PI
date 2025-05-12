package com.trafficsimulation.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.RenderingHints;
import java.awt.FontMetrics;

import com.trafficsimulation.model.*;
// import com.trafficsimulation.gui.VisualizationMode; // УДАЛЕН ИМПОРТ

public class SimulationPanel extends JPanel {

    private Road currentRoad;
    private double currentTime;
    private final int LANE_HEIGHT_PX = 30;
    private final int CAR_WIDTH_PX = 22;
    private final int CAR_HEIGHT_PX = 12;

    private Font carSpeedFont;
    private Font roadSignFont;
    private Font infoFont;
    private Font placementHintFont;
    private Font trafficLightTimerFont;
    // private Font carDebugFont; // УДАЛЕНО

    // private VisualizationMode currentVizMode = VisualizationMode.STANDARD; // УДАЛЕНО

    private boolean isInPlacementMode = false;
    private String placementObjectType = null;
    private Point mousePosition = null;

    public SimulationPanel() {
        setPreferredSize(new Dimension(1000, 400));
        setBackground(new Color(34, 139, 34));
        carSpeedFont = new Font("Arial", Font.BOLD, 10);
        roadSignFont = new Font("Arial", Font.BOLD, 9);
        infoFont = new Font("Arial", Font.PLAIN, 12);
        placementHintFont = new Font("Arial", Font.ITALIC, 12);
        trafficLightTimerFont = new Font("Monospaced", Font.BOLD, 10);
        // carDebugFont = new Font("Arial", Font.PLAIN, 9); // УДАЛЕНО

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (isInPlacementMode) {
                    mousePosition = e.getPoint();
                    repaint();
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (isInPlacementMode) {
                    mousePosition = null;
                    repaint();
                }
            }
        });
        System.out.println("SimulationPanel создана.");
    }

    public void updateSimulationState(Road road, double time) {
        this.currentRoad = road;
        this.currentTime = time;
        this.repaint();
    }

    public void setPlacementMode(boolean enabled, String objectType) {
        this.isInPlacementMode = enabled;
        this.placementObjectType = objectType;
        if (enabled) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
            mousePosition = null;
        }
        repaint();
    }

    // public void setVisualizationMode(VisualizationMode mode) { /* УДАЛЕН МЕТОД */ }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (currentRoad == null) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            String text = isInPlacementMode ? "Инициализируйте дорогу (Настройки/Старт) для расстановки." : "Инициализация...";
            FontMetrics fm = g2d.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(text, x, y);
        } else {
            drawRoadSurface(g2d);
            drawLanesAndMarkings(g2d);
            drawTrafficLights(g2d);
            drawRoadSigns(g2d);
            drawCars(g2d);
        }
        drawInfo(g2d);
        if (isInPlacementMode && placementObjectType != null && mousePosition != null) {
            drawPlacementHint(g2d);
        }
    }

    private void drawPlacementHint(Graphics2D g2d){
        if (mousePosition == null) return;
        g2d.setColor(Color.YELLOW);
        g2d.drawLine(mousePosition.x, 0, mousePosition.x, getHeight());
        g2d.setFont(placementHintFont);
        String text = "Клик: " + placementObjectType;
        g2d.drawString(text, mousePosition.x + 5, mousePosition.y - 5);
    }

    private void drawRoadSurface(Graphics2D g2d) {
        if (currentRoad == null) return;
        int totalLanes = currentRoad.getNumberOfLanes();
        int roadPixelHeight = totalLanes * LANE_HEIGHT_PX;
        int roadY = getHeight() / 2 - roadPixelHeight / 2;
        g2d.setColor(new Color(105, 105, 105));
        g2d.fillRect(0, roadY, getWidth(), roadPixelHeight);
    }

    private void drawLanesAndMarkings(Graphics2D g2d) {
        if (currentRoad == null) return;
        int totalLanes = currentRoad.getNumberOfLanes();
        int lanesPerDir = currentRoad.getNumberOfLanes() / currentRoad.getNumberOfDirections();
        int roadPixelHeight = totalLanes * LANE_HEIGHT_PX;
        int roadY = getHeight() / 2 - roadPixelHeight / 2;
        g2d.setColor(Color.WHITE);
        Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        Stroke solid = new BasicStroke(1);
        Stroke doubleSolidYellow = new BasicStroke(1);

        for (int i = 0; i < totalLanes; i++) {
            if (i == 0) continue;
            int lineY = roadY + i * LANE_HEIGHT_PX;
            if (currentRoad.getNumberOfDirections() == 2 && i == lanesPerDir) {
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(doubleSolidYellow);
                g2d.drawLine(0, lineY - 1, getWidth(), lineY - 1);
                g2d.drawLine(0, lineY + 1, getWidth(), lineY + 1);
            } else {
                g2d.setStroke(dashed);
                g2d.setColor(Color.WHITE);
                g2d.drawLine(0, lineY, getWidth(), lineY);
            }
        }
        g2d.setStroke(solid);
    }

    private void drawCars(Graphics2D g2d) {
        if (currentRoad == null || currentRoad.getCars() == null) return;
        int totalLanes = currentRoad.getNumberOfLanes();
        int roadPixelHeight = totalLanes * LANE_HEIGHT_PX;
        int roadY = getHeight() / 2 - roadPixelHeight / 2;
        g2d.setFont(carSpeedFont);
        FontMetrics fmCarSpeed = g2d.getFontMetrics();

        for (Car car : currentRoad.getCars()) {
            int carScreenX = (int) ((car.getPosition() / currentRoad.getLength()) * getWidth());
            int carScreenY = roadY + car.getLaneIndex() * LANE_HEIGHT_PX + (LANE_HEIGHT_PX - CAR_HEIGHT_PX) / 2;
            int carRectX = carScreenX - CAR_WIDTH_PX / 2;

            Color carBodyColor;
            Color carCabinColor;

            if (car.getDirection() == 0) {
                carBodyColor = new Color(0, 150, 255);
                carCabinColor = new Color(100, 200, 255);
            } else {
                carBodyColor = new Color(255, 60, 150);
                carCabinColor = new Color(255, 130, 200);
            }

            g2d.setColor(carBodyColor);
            g2d.fillRoundRect(carRectX, carScreenY, CAR_WIDTH_PX, CAR_HEIGHT_PX, 5, 5);
            int cabinWidth = CAR_WIDTH_PX * 2 / 3;
            int cabinHeight = CAR_HEIGHT_PX * 2 / 3;
            int cabinOffsetX = (CAR_WIDTH_PX - cabinWidth) / 2;
            int cabinOffsetY = (CAR_HEIGHT_PX - cabinHeight) / 4;
            g2d.setColor(carCabinColor);
            g2d.fillRoundRect(carRectX + cabinOffsetX, carScreenY + cabinOffsetY, cabinWidth, cabinHeight, 3,3);
            g2d.setColor(Color.BLACK);
            g2d.drawRoundRect(carRectX, carScreenY, CAR_WIDTH_PX, CAR_HEIGHT_PX, 5, 5);

            g2d.setColor(Color.BLACK);
            String speedText = String.format("%.0f", car.getCurrentSpeed() * 3.6);
            int textWidth = fmCarSpeed.stringWidth(speedText);
            int textX = carScreenX - textWidth / 2;
            int textY = carScreenY + fmCarSpeed.getAscent() - 1;
            g2d.drawString(speedText, textX, textY);
        }
    }

    private void drawTrafficLights(Graphics2D g2d) {
        if (currentRoad == null || currentRoad.getTrafficLights() == null) return;
        int roadPixelHeight = currentRoad.getNumberOfLanes() * LANE_HEIGHT_PX;
        int roadY = getHeight() / 2 - roadPixelHeight / 2;

        for (TrafficLight light : currentRoad.getTrafficLights()) {
            int lightScreenX = (int) ((light.getPosition() / currentRoad.getLength()) * getWidth());
            int lightSize = LANE_HEIGHT_PX / 2 + 2;

            int lightScreenY;
            int столбикYStart;
            int столбикHeight;

            if (light.getTargetDirection() == 0) { // Для движения слева-направо, светофор сверху
                lightScreenY = roadY - lightSize - 5;
                столбикYStart = lightScreenY + lightSize;
                столбикHeight = roadY - столбикYStart;
            } else { // Для движения справа-налево, светофор снизу
                lightScreenY = roadY + roadPixelHeight + 5;
                столбикYStart = roadY + roadPixelHeight;
                столбикHeight = lightScreenY - столбикYStart;
            }

            g2d.setColor(Color.DARK_GRAY.darker());
            g2d.fillRect(lightScreenX - 2, столбикYStart, 4, столбикHeight);

            switch (light.getCurrentState()) {
                case RED: g2d.setColor(Color.RED); break;
                case GREEN: g2d.setColor(Color.GREEN); break;
                default: g2d.setColor(Color.BLACK); break;
            }
            g2d.fillOval(lightScreenX - lightSize / 2, lightScreenY, lightSize, lightSize);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(lightScreenX - lightSize / 2, lightScreenY, lightSize, lightSize);

            String timeText = String.format("%.0f", light.getRemainingTime());
            g2d.setFont(trafficLightTimerFont);
            FontMetrics fmTimer = g2d.getFontMetrics();
            int textWidth = fmTimer.stringWidth(timeText);
            if (light.getCurrentState() == TrafficLightState.RED) g2d.setColor(Color.WHITE);
            else if (light.getCurrentState() == TrafficLightState.GREEN) g2d.setColor(Color.BLACK);
            else g2d.setColor(Color.DARK_GRAY);
            g2d.drawString(timeText, lightScreenX - textWidth / 2, lightScreenY + lightSize/2 + fmTimer.getAscent()/2 -1);
        }
    }

    private void drawRoadSigns(Graphics2D g2d) {
        if (currentRoad == null || currentRoad.getRoadSigns() == null) return;
        int roadPixelHeight = currentRoad.getNumberOfLanes() * LANE_HEIGHT_PX;
        int roadY = getHeight() / 2 - roadPixelHeight / 2;
        g2d.setFont(roadSignFont);
        FontMetrics fmSign = g2d.getFontMetrics();

        for(RoadSign sign : currentRoad.getRoadSigns()){
            int signX = (int) ( (sign.getPosition() / currentRoad.getLength()) * getWidth() );

            String signText = sign.getType().name().replace("SPEED_LIMIT_", "").replace("_SIGN_GENERIC", "INFO");
            int textWidth = fmSign.stringWidth(signText);
            int textHeightWithDescent = fmSign.getHeight();
            int textAscent = fmSign.getAscent();
            int padding = 4;
            int rectWidth = textWidth + 2 * padding;
            int rectHeight = textHeightWithDescent;
            int rectX = signX - rectWidth / 2;

            int signYBase; // Базовая точка для верха таблички знака
            int postEndY;  // Конец столбика

            if (sign.getTargetDirection() == 0) { // Для движения слева-направо, знак снизу
                signYBase = roadY + roadPixelHeight + 5;
                postEndY = signYBase; // Столбик идет до начала таблички снизу
            } else { // Для движения справа-налево, знак сверху
                signYBase = roadY - 20 - rectHeight; // 20 - отступ сверху до таблички
                postEndY = roadY - 5; // Столбик идет до обочины сверху
            }
            int rectY = signYBase;

            // Рисуем "столбик" знака
            g2d.setColor(Color.GRAY.darker());
            if (sign.getTargetDirection() == 0) { // Знак снизу, столбик от дороги до знака
                g2d.fillRect(signX - 1, roadY + roadPixelHeight, 2, rectY - (roadY + roadPixelHeight));
            } else { // Знак сверху, столбик от знака до дороги
                g2d.fillRect(signX - 1, rectY + rectHeight, 2, postEndY - (rectY + rectHeight));
            }

            if(sign.getType().name().contains("SPEED_LIMIT")){
                g2d.setColor(Color.WHITE);
                g2d.fillOval(rectX-padding, rectY-padding, rectWidth+2*padding, rectHeight+2*padding);
                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval(rectX-padding, rectY-padding, rectWidth+2*padding, rectHeight+2*padding);
                g2d.setColor(Color.BLACK);
            } else {
                g2d.setColor(Color.BLUE);
                g2d.fillRect(rectX, rectY, rectWidth, rectHeight);
                g2d.setColor(Color.WHITE);
                g2d.drawRect(rectX, rectY, rectWidth, rectHeight);
            }
            g2d.drawString(signText, rectX + padding, rectY + textAscent + padding/2 -1);
            g2d.setStroke(new BasicStroke(1));
        }
    }

    private void drawInfo(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(infoFont);
        g2d.drawString(String.format("Время: %.2f c", currentTime), 10, 20);
        g2d.drawString("Машин: " + (currentRoad != null && currentRoad.getCars() != null ? currentRoad.getCars().size() : 0), 10, 35);
        if (currentRoad != null) {
            g2d.drawString("Дорога: " + currentRoad.getType() + ", " +
                    (currentRoad.getNumberOfDirections() == 1 ? "1-стор., " : "2-стор., ") +
                    currentRoad.getNumberOfLanes()/currentRoad.getNumberOfDirections() + " полос/напр.", 10, 50);
        }
    }
}