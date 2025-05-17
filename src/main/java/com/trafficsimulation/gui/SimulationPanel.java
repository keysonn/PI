package com.trafficsimulation.gui;

import com.trafficsimulation.model.Car;
import com.trafficsimulation.model.Road;
import com.trafficsimulation.model.RoadSign;
import com.trafficsimulation.model.RoadSignType;
import com.trafficsimulation.model.RoadType;
import com.trafficsimulation.model.TrafficLight;
import com.trafficsimulation.model.TrafficLightState;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class SimulationPanel extends JPanel {

    private Road road;
    private double simulationTime;

    private boolean placementModeActive = false;
    private String placementHint = null;

    private static final int ROAD_RENDER_HEIGHT = 80;
    private static final Color ROAD_COLOR = new Color(100, 100, 100);
    private static final Color LANE_SEPARATOR_COLOR = Color.WHITE;
    private static final Color CENTER_LINE_COLOR = Color.YELLOW;
    private static final Color GRASS_COLOR = new Color(34, 139, 34);

    private static final int TRAFFIC_LIGHT_VISUAL_WIDTH = 20;
    private static final int TRAFFIC_LIGHT_POLE_HEIGHT = 30;
    private static final Color TRAFFIC_LIGHT_POLE_COLOR = Color.DARK_GRAY;
    private static final int OBJECT_SIDE_OFFSET = 5;

    private static final int CAR_RENDER_WIDTH = 32;
    private static final int CAR_RENDER_HEIGHT = 18;
    private static final int CAR_ARC_RADIUS = 10;
    private static final Color CAR_WINDOW_COLOR = new Color(173, 216, 230, 180);

    private static final int ROAD_SIGN_SIZE = 24;
    private static final int ROAD_SIGN_POLE_HEIGHT = 30;
    private static final Color ROAD_SIGN_POLE_COLOR = Color.DARK_GRAY;
    private static final Color ROAD_SIGN_BG_COLOR = Color.WHITE;
    private static final Color ROAD_SIGN_BORDER_COLOR = Color.RED;
    private static final Color ROAD_SIGN_TEXT_COLOR = Color.BLACK;

    private final Stroke dashedStroke = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[]{12, 8}, 0);
    private final Stroke solidThinStroke = new BasicStroke(2);
    private final Stroke solidThickStroke = new BasicStroke(3);


    public SimulationPanel() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(GRASS_COLOR);
    }

    public void updateSimulationState(Road road, double simulationTime) {
        this.road = road;
        this.simulationTime = simulationTime;
        repaint();
    }

    public void setPlacementMode(boolean active, String hint) {
        this.placementModeActive = active;
        this.placementHint = hint;
        if (active) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        if (road == null) {
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            String message = "Дорога не инициализирована. Задайте параметры и запустите генерацию.";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(message, getWidth() / 2 - fm.stringWidth(message) / 2, getHeight() / 2);
            return;
        }

        int roadVisualTopY = getHeight() / 2 - ROAD_RENDER_HEIGHT / 2;

        drawRoadSurfaceAndMarkings(g2d, roadVisualTopY);

        if (road.getCars() != null && !road.getCars().isEmpty()) {
            for (Car car : road.getCars()) {
                drawCar(g2d, car, roadVisualTopY);
            }
        }

        if (road.getTrafficLights() != null && !road.getTrafficLights().isEmpty()) {
            int panelWidth = getWidth();
            for (TrafficLight light : road.getTrafficLights()) {
                int lightScreenX;
                int lightSignalTopY;

                // Модельное направление света (0 для L->R, 1 для R->L)
                int lightModelDirection = light.getTargetDirection();
                boolean placeLightAboveRoad;

                if (road.getType() == RoadType.TUNNEL) {
                    // В тоннеле системные светофоры: model_dir 1 (R->L) -> сверху; model_dir 0 (L->R) -> снизу
                    placeLightAboveRoad = (lightModelDirection == 1);
                } else {
                    // Обычная дорога: model_dir 1 (R->L, верхний на экране) -> светофор сверху
                    // model_dir 0 (L->R, нижний на экране) -> светофор снизу
                    placeLightAboveRoad = (lightModelDirection == 1);
                }
                // Если светофор для обоих направлений (-1), рисуем его сверху по умолчанию
                if (lightModelDirection == -1 && road.getNumberOfDirections() == 2) {
                    placeLightAboveRoad = true; // Главный сверху
                    // Можно было бы нарисовать и второй снизу, но пока один.
                }


                if (placeLightAboveRoad) {
                    lightSignalTopY = roadVisualTopY - OBJECT_SIDE_OFFSET - TRAFFIC_LIGHT_POLE_HEIGHT - TRAFFIC_LIGHT_VISUAL_WIDTH;
                } else {
                    lightSignalTopY = roadVisualTopY + ROAD_RENDER_HEIGHT + OBJECT_SIDE_OFFSET + TRAFFIC_LIGHT_POLE_HEIGHT;
                }

                double roadModelLength = road.getLength();
                if (roadModelLength > 0) {
                    lightScreenX = (int) ((light.getPosition() / roadModelLength) * panelWidth);
                    lightScreenX -= TRAFFIC_LIGHT_VISUAL_WIDTH / 2;
                    lightScreenX = Math.max(0, Math.min(lightScreenX, panelWidth - TRAFFIC_LIGHT_VISUAL_WIDTH));
                } else {
                    lightScreenX = 10;
                }
                drawTrafficLight(g2d, light, lightScreenX, lightSignalTopY, roadVisualTopY);
            }
        }

        if (road.getRoadSigns() != null && !road.getRoadSigns().isEmpty()){
            int panelWidth = getWidth();
            for (RoadSign sign : road.getRoadSigns()) {
                int signScreenX;
                int signTopY;
                int signModelDirection = sign.getTargetDirection();
                boolean placeSignAboveRoad;

                if (road.getType() == RoadType.TUNNEL) {
                    // В тоннеле знаки (если бы были) обычно для обоих направлений, ставим сверху
                    placeSignAboveRoad = true;
                } else {
                    // model_dir 1 (R->L, верхний на экране) -> знак сверху
                    // model_dir 0 (L->R, нижний на экране) -> знак снизу
                    placeSignAboveRoad = (signModelDirection == 1);
                }
                if (signModelDirection == -1 && road.getNumberOfDirections() == 2) { // Знак для обоих направлений
                    placeSignAboveRoad = true; // Основной сверху
                    // Можно нарисовать и второй снизу
                    int नीचेSignTopY = roadVisualTopY + ROAD_RENDER_HEIGHT + OBJECT_SIDE_OFFSET + ROAD_SIGN_POLE_HEIGHT;
                    int tempSignX = (int) ((sign.getPosition() / road.getLength()) * panelWidth) - ROAD_SIGN_SIZE / 2;
                    tempSignX = Math.max(0, Math.min(tempSignX, panelWidth - ROAD_SIGN_SIZE));
                    drawRoadSign(g2d, sign, tempSignX, नीचेSignTopY, roadVisualTopY); // Рисуем второй снизу
                }


                if (placeSignAboveRoad) {
                    signTopY = roadVisualTopY - OBJECT_SIDE_OFFSET - ROAD_SIGN_POLE_HEIGHT - ROAD_SIGN_SIZE;
                } else { // Только если это не знак -1, который уже нарисован сверху
                    if (signModelDirection != -1 || road.getNumberOfDirections() == 1) { // Для односторонней - всегда снизу если modelDir 0
                        signTopY = roadVisualTopY + ROAD_RENDER_HEIGHT + OBJECT_SIDE_OFFSET + ROAD_SIGN_POLE_HEIGHT;
                    } else {
                        continue; // Знак -1 для двухсторонки уже нарисован сверху, второй нарисован выше
                    }
                }


                double roadModelLength = road.getLength();
                if (roadModelLength > 0) {
                    signScreenX = (int) ((sign.getPosition() / roadModelLength) * panelWidth);
                    signScreenX -= ROAD_SIGN_SIZE / 2;
                    signScreenX = Math.max(0, Math.min(signScreenX, panelWidth - ROAD_SIGN_SIZE));
                } else {
                    signScreenX = 20;
                }
                drawRoadSign(g2d, sign, signScreenX, signTopY, roadVisualTopY);
            }
        }

        drawInfoPanel(g2d);
        if (placementModeActive && placementHint != null) {
            g2d.setColor(new Color(0,0,200, 200));
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            String hintText = "РЕЖИМ: " + placementHint + ". Кликните на дорогу.";
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(hintText);
            g2d.fillRect(getWidth() / 2 - textWidth / 2 - 10, getHeight() - 65 - fm.getAscent(), textWidth + 20, fm.getHeight() + 5);
            g2d.setColor(Color.WHITE);
            g2d.drawString(hintText, getWidth() / 2 - textWidth / 2, getHeight() - 60);
        }
    }

    private void drawRoadSurfaceAndMarkings(Graphics2D g2d, int roadVisualTopY) {
        g2d.setColor(ROAD_COLOR);
        g2d.fillRect(0, roadVisualTopY, getWidth(), ROAD_RENDER_HEIGHT);

        int lanesPerModelDir = road.getLanesPerDirection(); // Полос в каждом модельном направлении
        if (lanesPerModelDir == 0) lanesPerModelDir = 1;

        int totalLanesOnScreen = road.getNumberOfLanes(); // Общее число полос
        if (totalLanesOnScreen == 0) totalLanesOnScreen = 1;

        int laneVisualHeight = ROAD_RENDER_HEIGHT / totalLanesOnScreen;

        g2d.setColor(Color.WHITE);
        g2d.setStroke(solidThinStroke);
        g2d.drawLine(0, roadVisualTopY, getWidth(), roadVisualTopY);
        g2d.drawLine(0, roadVisualTopY + ROAD_RENDER_HEIGHT, getWidth(), roadVisualTopY + ROAD_RENDER_HEIGHT);

        if (road.getType() == RoadType.TUNNEL) {
            g2d.setColor(CENTER_LINE_COLOR);
            g2d.setStroke(solidThickStroke);
            // Осевая линия всегда посередине ROAD_RENDER_HEIGHT
            g2d.drawLine(0, roadVisualTopY + ROAD_RENDER_HEIGHT / 2, getWidth(), roadVisualTopY + ROAD_RENDER_HEIGHT / 2);
        } else if (road.getNumberOfDirections() == 1) { // Односторонняя (в модели всегда dir=0, рисуется внизу)
            g2d.setColor(LANE_SEPARATOR_COLOR);
            g2d.setStroke(dashedStroke);
            // Полосы отсчитываются от roadVisualTopY
            for (int i = 1; i < lanesPerModelDir; i++) { // lanesPerModelDir == totalLanesOnScreen
                int lineY = roadVisualTopY + i * laneVisualHeight;
                g2d.drawLine(0, lineY, getWidth(), lineY);
            }
        } else if (road.getNumberOfDirections() == 2) { // Двухсторонняя
            g2d.setColor(CENTER_LINE_COLOR);
            g2d.setStroke(solidThinStroke);
            // Осевая линия между модельным направлением 1 (верхний блок) и 0 (нижний блок)
            int centerLineY = roadVisualTopY + lanesPerModelDir * laneVisualHeight;
            int gapBetweenDoubleLines = 3;
            g2d.drawLine(0, centerLineY - gapBetweenDoubleLines / 2, getWidth(), centerLineY - gapBetweenDoubleLines / 2);
            g2d.drawLine(0, centerLineY + gapBetweenDoubleLines / 2, getWidth(), centerLineY + gapBetweenDoubleLines / 2);

            g2d.setColor(LANE_SEPARATOR_COLOR);
            g2d.setStroke(dashedStroke);
            // Разделительные для ВЕРХНЕГО блока (model_dir=1)
            for (int i = 1; i < lanesPerModelDir; i++) {
                int lineY_screenTopBlock = roadVisualTopY + i * laneVisualHeight;
                g2d.drawLine(0, lineY_screenTopBlock, getWidth(), lineY_screenTopBlock);
            }
            // Разделительные для НИЖНЕГО блока (model_dir=0)
            int bottomBlockStartY = roadVisualTopY + lanesPerModelDir * laneVisualHeight;
            for (int i = 1; i < lanesPerModelDir; i++) {
                int lineY_screenBottomBlock = bottomBlockStartY + i * laneVisualHeight;
                g2d.drawLine(0, lineY_screenBottomBlock, getWidth(), lineY_screenBottomBlock);
            }
        }
    }

    private void drawTrafficLight(Graphics2D g2d, TrafficLight light, int screenX, int screenY_signal_top, int roadVisualTopY) {
        int signalDiameter = TRAFFIC_LIGHT_VISUAL_WIDTH;
        int signalRadius = signalDiameter / 2;
        g2d.setColor(TRAFFIC_LIGHT_POLE_COLOR);
        int poleX = screenX + signalRadius - 2;
        boolean isAboveRoad = screenY_signal_top < roadVisualTopY;

        if (isAboveRoad) {
            g2d.fillRect(poleX, roadVisualTopY - TRAFFIC_LIGHT_POLE_HEIGHT - OBJECT_SIDE_OFFSET, 4, TRAFFIC_LIGHT_POLE_HEIGHT);
        } else {
            g2d.fillRect(poleX, roadVisualTopY + ROAD_RENDER_HEIGHT + OBJECT_SIDE_OFFSET, 4, TRAFFIC_LIGHT_POLE_HEIGHT);
        }

        Color signalColor;
        if (light.getCurrentState() == TrafficLightState.GREEN) signalColor = Color.GREEN.brighter();
        else if (light.getCurrentState() == TrafficLightState.RED) signalColor = Color.RED.brighter();
        else signalColor = Color.GRAY; // Should not happen if logic is correct
        g2d.setColor(signalColor);
        g2d.fillOval(screenX, screenY_signal_top, signalDiameter, signalDiameter);

        Color textColor = (light.getCurrentState() == TrafficLightState.GREEN || light.getCurrentState() == null) ? Color.BLACK : Color.WHITE;
        g2d.setColor(textColor);
        g2d.setFont(new Font("Arial", Font.BOLD, Math.max(9, signalRadius - 2)));
        String timeText = String.format("%.0f", light.getRemainingTime());
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(timeText);
        g2d.drawString(timeText,
                screenX + signalRadius - textWidth / 2,
                screenY_signal_top + signalRadius + fm.getAscent() / 2 -1);
    }

    private void drawCar(Graphics2D g2d, Car car, int roadVisualTopY) {
        double roadModelLength = road.getLength();
        int panelWidth = getWidth();
        int carScreenX;

        if (roadModelLength > 0) {
            carScreenX = (int) ((car.getPosition() / roadModelLength) * panelWidth);
        } else {
            // Защита, если длина дороги 0
            carScreenX = (car.getDirection() == 0) ? -CAR_RENDER_WIDTH : panelWidth + CAR_RENDER_WIDTH;
        }
        carScreenX -= CAR_RENDER_WIDTH / 2; // Центрирование машины по X

        int totalLanesOnScreen = road.getNumberOfLanes();
        if (totalLanesOnScreen == 0) totalLanesOnScreen = 1;
        int laneVisualHeight = ROAD_RENDER_HEIGHT / totalLanesOnScreen;

        // Используем модельное направление машины и ее локальный индекс для получения глобального индекса отрисовки
        int globalLaneForDrawing = road.getGlobalLaneIndexForDrawing(car.getLaneIndex(), car.getDirection());

        if (globalLaneForDrawing == -1) {
            System.err.println("Error drawing car " + car.getId() + ": Invalid global lane index from local " + car.getLaneIndex() + " model_dir " + car.getDirection());
            return;
        }

        int carScreenY = roadVisualTopY + (globalLaneForDrawing * laneVisualHeight) + (laneVisualHeight / 2 - CAR_RENDER_HEIGHT / 2);

        Color carBodyColor;
        // Цвет теперь зависит от модельного направления
        if (car.getDirection() == 1) { // model_dir 1 (R->L, верхний на экране) - синий
            carBodyColor = new Color(50, 90, 180);
        } else { // model_dir 0 (L->R, нижний на экране) - красный
            carBodyColor = new Color(180, 50, 90);
        }

        Shape carShape = new RoundRectangle2D.Double(carScreenX, carScreenY, CAR_RENDER_WIDTH, CAR_RENDER_HEIGHT, CAR_ARC_RADIUS, CAR_ARC_RADIUS);
        g2d.setColor(carBodyColor);
        g2d.fill(carShape);

        g2d.setColor(CAR_WINDOW_COLOR);
        int windowWidth = CAR_RENDER_WIDTH / 2;
        int windowHeight = CAR_RENDER_HEIGHT / 2 - 2;
        // Лобовое стекло:
        // Если модельное направление 0 (L->R на экране), то стекло справа
        // Если модельное направление 1 (R->L на экране), то стекло слева
        int windowXOffset = (car.getDirection() == 0) ? CAR_RENDER_WIDTH / 2 - 2 : 2;
        g2d.fillRoundRect(carScreenX + windowXOffset , carScreenY + 2, windowWidth, windowHeight, CAR_ARC_RADIUS / 2, CAR_ARC_RADIUS / 2);

        g2d.setColor(carBodyColor.darker());
        g2d.draw(carShape);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
        String speedText = String.format("%.0f", car.getCurrentSpeed() * 3.6); // км/ч
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(speedText,
                carScreenX + CAR_RENDER_WIDTH / 2 - fm.stringWidth(speedText) / 2,
                carScreenY + CAR_RENDER_HEIGHT / 2 + fm.getAscent() / 2 - 2);
    }

    private void drawInfoPanel(Graphics2D g2d) {
        g2d.setColor(new Color(0,0,0,150));
        g2d.fillRect(5,5, 250, 50);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        String simTimeStr = String.format("Время: %.1f c", simulationTime);
        String carCountStr = "Машин: " + (road.getCars() != null ? road.getCars().size() : 0);
        String roadInfoStr;
        if (road.getType() == RoadType.TUNNEL) {
            roadInfoStr = "Дорога: " + road.getType() + " (реверс.)";
        } else {
            int lanesPerDirInfo = road.getLanesPerDirection();
            if (lanesPerDirInfo == 0) lanesPerDirInfo = 1;
            roadInfoStr = "Дорога: " + road.getType() + ", " +
                    (road.getNumberOfDirections() == 1 ? "1-стор." : "2-стор.") + ", " +
                    lanesPerDirInfo + " п./напр.";
        }
        g2d.drawString(simTimeStr, 10, 20);
        g2d.drawString(carCountStr, 10, 35);
        g2d.drawString(roadInfoStr, 10, 50);
    }

    private void drawRoadSign(Graphics2D g2d, RoadSign sign, int screenX, int screenY_sign_top, int roadVisualTopY) {
        g2d.setColor(ROAD_SIGN_POLE_COLOR);
        int poleX = screenX + ROAD_SIGN_SIZE / 2 - 2;
        boolean isAboveRoad = screenY_sign_top < roadVisualTopY;

        if (isAboveRoad) {
            g2d.fillRect(poleX, roadVisualTopY - ROAD_SIGN_POLE_HEIGHT - OBJECT_SIDE_OFFSET, 4, ROAD_SIGN_POLE_HEIGHT);
        } else {
            g2d.fillRect(poleX, roadVisualTopY + ROAD_RENDER_HEIGHT + OBJECT_SIDE_OFFSET, 4, ROAD_SIGN_POLE_HEIGHT);
        }

        if (sign.getType() == RoadSignType.SPEED_LIMIT) {
            g2d.setColor(ROAD_SIGN_BG_COLOR);
            g2d.fillOval(screenX, screenY_sign_top, ROAD_SIGN_SIZE, ROAD_SIGN_SIZE);
            g2d.setColor(ROAD_SIGN_BORDER_COLOR);
            g2d.setStroke(new BasicStroke(Math.max(1, ROAD_SIGN_SIZE / 10f)));
            g2d.drawOval(screenX, screenY_sign_top, ROAD_SIGN_SIZE, ROAD_SIGN_SIZE);
            g2d.setStroke(new BasicStroke(1)); // Reset stroke
            g2d.setColor(ROAD_SIGN_TEXT_COLOR);
            String text = String.format("%.0f", sign.getSpeedLimitKmh());
            int fontSize = ROAD_SIGN_SIZE / 2;
            if (sign.getSpeedLimitKmh() >= 100) fontSize = (int)(ROAD_SIGN_SIZE / 2.3f);
            else if (sign.getSpeedLimitKmh() < 10) fontSize = (int)(ROAD_SIGN_SIZE / 1.9f);
            g2d.setFont(new Font("Arial", Font.BOLD, fontSize));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            g2d.drawString(text,
                    screenX + ROAD_SIGN_SIZE / 2 - textWidth / 2,
                    screenY_sign_top + ROAD_SIGN_SIZE / 2 + fm.getAscent() / 2 - fm.getDescent() / 3);
        } else { // Fallback for unknown sign types
            g2d.setColor(Color.BLUE);
            g2d.fillRect(screenX, screenY_sign_top, ROAD_SIGN_SIZE, ROAD_SIGN_SIZE);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, ROAD_SIGN_SIZE - 8));
            g2d.drawString("?", screenX + ROAD_SIGN_SIZE/2 - g2d.getFontMetrics().stringWidth("?")/2,
                    screenY_sign_top + ROAD_SIGN_SIZE/2 + g2d.getFontMetrics().getAscent()/2 - g2d.getFontMetrics().getDescent()/2);
        }
    }
}