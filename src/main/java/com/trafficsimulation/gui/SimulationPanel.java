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
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

public class SimulationPanel extends JPanel {

    private Road road;
    private double simulationTime;

    private boolean placementModeActive = false;
    private String placementHint = null;

    public static final int TARGET_LANE_VISUAL_HEIGHT = 40;

    private static final Color ROAD_COLOR = new Color(100, 100, 100);
    private static final Color LANE_SEPARATOR_COLOR = Color.WHITE;
    private static final Color CENTER_LINE_COLOR = Color.YELLOW;
    private static final Color GRASS_COLOR = new Color(34, 139, 34);
    private static final Color DARK_GRASS_COLOR = new Color(20, 80, 20);
    private static final Color SHOULDER_COLOR = new Color(130, 130, 130);
    private static final int SHOULDER_WIDTH = 10;

    public static final int TRAFFIC_LIGHT_VISUAL_WIDTH = 20;
    public static final int TRAFFIC_LIGHT_POLE_HEIGHT = 30;
    private static final Color TRAFFIC_LIGHT_POLE_COLOR = Color.DARK_GRAY;
    public static final int OBJECT_SIDE_OFFSET = 5;

    private static final int CAR_RENDER_WIDTH = 36;
    private static final int CAR_RENDER_HEIGHT = 16;
    private static final int CAR_ARC_RADIUS = 8;
    private static final Color CAR_WINDOW_COLOR = new Color(173, 216, 230, 180);
    private static final double MAX_CAR_TILT_ANGLE = Math.toRadians(4);
    private static final Color BRAKE_LIGHT_COLOR = new Color(255, 0, 0, 200);
    private static final int BRAKE_LIGHT_SIZE = CAR_RENDER_HEIGHT / 3;

    private static final Color WHEEL_COLOR = Color.BLACK;
    private static final int WHEEL_DIAMETER = 7;
    private static final int WHEEL_OFFSET_X = 5;
    private static final int WHEEL_OFFSET_Y = 1;


    public static final int ROAD_SIGN_SIZE = 24;
    public static final int ROAD_SIGN_POLE_HEIGHT = 30;
    private static final Color ROAD_SIGN_POLE_COLOR = Color.DARK_GRAY;
    private static final Color ROAD_SIGN_BG_COLOR = Color.WHITE;
    private static final Color ROAD_SIGN_BORDER_COLOR = Color.RED;
    private static final Color ROAD_SIGN_TEXT_COLOR = Color.BLACK;

    private static final Color TUNNEL_WALL_COLOR = new Color(140, 140, 140);
    private static final Color TUNNEL_INTERIOR_OVERLAY_COLOR = new Color(50, 50, 70, 50);
    private static final Color TUNNEL_LIGHT_COLOR = new Color(255, 255, 200, 220);
    private static final int TUNNEL_LIGHT_WIDTH = TARGET_LANE_VISUAL_HEIGHT / 2;
    private static final int TUNNEL_LIGHT_HEIGHT = 6;
    private static final int TUNNEL_LIGHT_SPACING_PIXELS = 120;

    private final Stroke dashedStroke = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[]{12, 8}, 0);
    private final Stroke solidThinStroke = new BasicStroke(2);
    private final Stroke solidThickStroke = new BasicStroke(3);

    public SimulationPanel() {
        setPreferredSize(new Dimension(800, 600));
    }

    public void updateSimulationState(Road road, double simulationTime) {
        this.road = road;
        this.simulationTime = simulationTime;
        repaint();
    }

    public void setPlacementMode(boolean active, String hint) {
        this.placementModeActive = active;
        this.placementHint = hint;
        setCursor(active ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) : Cursor.getDefaultCursor());
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        boolean isTunnel = (road != null && road.getType() == RoadType.TUNNEL);
        setBackground(isTunnel ? DARK_GRASS_COLOR : GRASS_COLOR);

        if (road == null) {
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            String message = "Дорога не инициализирована. Задайте параметры и запустите генерацию.";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(message, getWidth() / 2 - fm.stringWidth(message) / 2, getHeight() / 2);
            return;
        }

        int totalLanesOnScreen = road.getNumberOfLanes();
        if (totalLanesOnScreen == 0) totalLanesOnScreen = 1;

        int currentRoadRenderHeight = TARGET_LANE_VISUAL_HEIGHT * totalLanesOnScreen;
        int roadVisualTopY = getHeight() / 2 - currentRoadRenderHeight / 2;

        g2d.setColor(SHOULDER_COLOR);
        g2d.fillRect(0, roadVisualTopY - SHOULDER_WIDTH, getWidth(), SHOULDER_WIDTH);
        g2d.fillRect(0, roadVisualTopY + currentRoadRenderHeight, getWidth(), SHOULDER_WIDTH);

        drawRoadSurfaceAndMarkings(g2d, roadVisualTopY, currentRoadRenderHeight);

        if (isTunnel) {
            drawTunnelInteriorOverlay(g2d, roadVisualTopY, currentRoadRenderHeight);
            drawTunnelFeatures(g2d, roadVisualTopY, currentRoadRenderHeight);
        }

        if (road.getCars() != null) {
            for (Car car : road.getCars()) {
                drawCar(g2d, car, roadVisualTopY, TARGET_LANE_VISUAL_HEIGHT);
            }
        }
        drawRoadObjects(g2d, roadVisualTopY, currentRoadRenderHeight);
        drawInfoPanel(g2d);
        drawPlacementHint(g2d, currentRoadRenderHeight);
    }

    private void drawRoadSurfaceAndMarkings(Graphics2D g2d, int roadVisualTopY, int currentRoadRenderHeight) {
        g2d.setColor(ROAD_COLOR);
        g2d.fillRect(0, roadVisualTopY, getWidth(), currentRoadRenderHeight);
        int lanesPerModelDir = road.getLanesPerDirection();
        if (lanesPerModelDir == 0) lanesPerModelDir = 1;
        int laneVisualHeight = TARGET_LANE_VISUAL_HEIGHT;

        g2d.setColor(Color.WHITE);
        g2d.setStroke(solidThinStroke);
        g2d.drawLine(0, roadVisualTopY, getWidth(), roadVisualTopY);
        g2d.drawLine(0, roadVisualTopY + currentRoadRenderHeight, getWidth(), roadVisualTopY + currentRoadRenderHeight);

        if (road.getType() == RoadType.TUNNEL) {
            g2d.setColor(CENTER_LINE_COLOR);
            g2d.setStroke(solidThickStroke);
            g2d.drawLine(0, roadVisualTopY + currentRoadRenderHeight / 2, getWidth(), roadVisualTopY + currentRoadRenderHeight / 2);
        } else if (road.getNumberOfDirections() == 1) {
            g2d.setColor(LANE_SEPARATOR_COLOR);
            g2d.setStroke(dashedStroke);
            for (int i = 1; i < lanesPerModelDir; i++) {
                g2d.drawLine(0, roadVisualTopY + i * laneVisualHeight, getWidth(), roadVisualTopY + i * laneVisualHeight);
            }
        } else if (road.getNumberOfDirections() == 2) {
            g2d.setColor(CENTER_LINE_COLOR);
            g2d.setStroke(solidThinStroke);
            int centerLineY = roadVisualTopY + lanesPerModelDir * laneVisualHeight;
            int gap = 3;
            g2d.drawLine(0, centerLineY - gap / 2, getWidth(), centerLineY - gap / 2);
            g2d.drawLine(0, centerLineY + gap / 2, getWidth(), centerLineY + gap / 2);
            g2d.setColor(LANE_SEPARATOR_COLOR);
            g2d.setStroke(dashedStroke);
            for (int i = 1; i < lanesPerModelDir; i++) {
                g2d.drawLine(0, roadVisualTopY + i * laneVisualHeight, getWidth(), roadVisualTopY + i * laneVisualHeight);
                g2d.drawLine(0, roadVisualTopY + lanesPerModelDir * laneVisualHeight + i * laneVisualHeight, getWidth(), roadVisualTopY + lanesPerModelDir * laneVisualHeight + i * laneVisualHeight);
            }
        }
    }

    private void drawTunnelInteriorOverlay(Graphics2D g2d, int roadVisualTopY, int currentRoadRenderHeight){
        if (road == null || road.getLength() <= 0) return;
        double roadLengthModel = road.getLength();
        int panelWidth = getWidth();
        double startTunnelPercent = 0.1;
        double endTunnelPercent = 0.9;
        int startX = (int) (startTunnelPercent * panelWidth);
        int endX = (int) (endTunnelPercent * panelWidth);
        int tunnelSectionWidth = endX - startX;
        if (tunnelSectionWidth > 0) {
            g2d.setColor(TUNNEL_INTERIOR_OVERLAY_COLOR);
            g2d.fillRect(startX, roadVisualTopY, tunnelSectionWidth, currentRoadRenderHeight);
        }
    }

    private void drawTunnelFeatures(Graphics2D g2d, int roadVisualTopY, int currentRoadRenderHeight) {
        int wallThickness = 30;
        g2d.setColor(TUNNEL_WALL_COLOR);
        g2d.fillRect(0, roadVisualTopY - wallThickness - SHOULDER_WIDTH, getWidth(), wallThickness);
        g2d.fillRect(0, roadVisualTopY + currentRoadRenderHeight + SHOULDER_WIDTH, getWidth(), wallThickness);
        g2d.setColor(TUNNEL_LIGHT_COLOR);
        int lightY = roadVisualTopY - wallThickness - SHOULDER_WIDTH - TUNNEL_LIGHT_HEIGHT - 2;
        for (int x = TUNNEL_LIGHT_SPACING_PIXELS / 2; x < getWidth(); x += TUNNEL_LIGHT_SPACING_PIXELS) {
            g2d.fillRect(x - TUNNEL_LIGHT_WIDTH / 2, lightY, TUNNEL_LIGHT_WIDTH, TUNNEL_LIGHT_HEIGHT);
        }
    }

    private void drawRoadObjects(Graphics2D g2d, int roadVisualTopY, int currentRoadRenderHeight) {
        int panelWidth = getWidth();
        if (road.getTrafficLights() != null) {
            for (TrafficLight light : road.getTrafficLights()) {
                int lightModelDir = light.getTargetDirection();
                boolean placeAbove = (road.getType() == RoadType.TUNNEL) ? (lightModelDir == 1) : (lightModelDir == 1 || lightModelDir == -1);
                int lightSignalTopY = placeAbove ?
                        roadVisualTopY - SHOULDER_WIDTH - OBJECT_SIDE_OFFSET - TRAFFIC_LIGHT_POLE_HEIGHT - TRAFFIC_LIGHT_VISUAL_WIDTH :
                        roadVisualTopY + currentRoadRenderHeight + SHOULDER_WIDTH + OBJECT_SIDE_OFFSET + TRAFFIC_LIGHT_POLE_HEIGHT;
                int lightScreenX = (int) ((light.getPosition() / road.getLength()) * panelWidth) - TRAFFIC_LIGHT_VISUAL_WIDTH / 2;
                lightScreenX = Math.max(0, Math.min(lightScreenX, panelWidth - TRAFFIC_LIGHT_VISUAL_WIDTH));
                drawTrafficLight(g2d, light, lightScreenX, lightSignalTopY, roadVisualTopY, currentRoadRenderHeight, placeAbove);
            }
        }
        if (road.getRoadSigns() != null) {
            for (RoadSign sign : road.getRoadSigns()) {
                int signModelDir = sign.getTargetDirection();
                boolean primaryPlaceAbove = (road.getType() == RoadType.TUNNEL) || (signModelDir == 1) || (signModelDir == -1 && road.getNumberOfDirections() == 2);
                int signTopY = primaryPlaceAbove ?
                        roadVisualTopY - SHOULDER_WIDTH - OBJECT_SIDE_OFFSET - ROAD_SIGN_POLE_HEIGHT - ROAD_SIGN_SIZE :
                        roadVisualTopY + currentRoadRenderHeight + SHOULDER_WIDTH + OBJECT_SIDE_OFFSET + ROAD_SIGN_POLE_HEIGHT;
                int signScreenX = (int) ((sign.getPosition() / road.getLength()) * panelWidth) - ROAD_SIGN_SIZE / 2;
                signScreenX = Math.max(0, Math.min(signScreenX, panelWidth - ROAD_SIGN_SIZE));
                drawRoadSign(g2d, sign, signScreenX, signTopY, roadVisualTopY, currentRoadRenderHeight, primaryPlaceAbove);
                if (signModelDir == -1 && road.getNumberOfDirections() == 2 && primaryPlaceAbove && road.getType() != RoadType.TUNNEL) {
                    int bottomSignTopY = roadVisualTopY + currentRoadRenderHeight + SHOULDER_WIDTH + OBJECT_SIDE_OFFSET + ROAD_SIGN_POLE_HEIGHT;
                    drawRoadSign(g2d, sign, signScreenX, bottomSignTopY, roadVisualTopY, currentRoadRenderHeight, false);
                }
            }
        }
    }

    private void drawTrafficLight(Graphics2D g2d, TrafficLight light, int screenX, int screenY_signal_top, int roadVisualTopY, int currentRoadRenderHeight, boolean isAboveRoad) {
        int signalDiameter = TRAFFIC_LIGHT_VISUAL_WIDTH;
        g2d.setColor(TRAFFIC_LIGHT_POLE_COLOR);
        int poleX = screenX + signalDiameter / 2 - 2;
        int poleBaseY = isAboveRoad ? (roadVisualTopY - SHOULDER_WIDTH - OBJECT_SIDE_OFFSET) : (roadVisualTopY + currentRoadRenderHeight + SHOULDER_WIDTH + OBJECT_SIDE_OFFSET);
        int poleTopY = isAboveRoad ? (poleBaseY - TRAFFIC_LIGHT_POLE_HEIGHT) : poleBaseY;
        int poleRenderHeight = TRAFFIC_LIGHT_POLE_HEIGHT;
        if(isAboveRoad) g2d.fillRect(poleX, poleTopY, 4, poleRenderHeight);
        else g2d.fillRect(poleX, poleBaseY, 4, poleRenderHeight);
        g2d.setColor(light.getCurrentState() == TrafficLightState.GREEN ? Color.GREEN.brighter() : (light.getCurrentState() == TrafficLightState.RED ? Color.RED.brighter() : Color.GRAY));
        g2d.fillOval(screenX, screenY_signal_top, signalDiameter, signalDiameter);
        g2d.setColor(light.getCurrentState() == TrafficLightState.GREEN ? Color.BLACK : Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, Math.max(9, signalDiameter / 2 - 2)));
        String timeText = String.format("%.0f", light.getRemainingTime());
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(timeText, screenX + signalDiameter / 2 - fm.stringWidth(timeText) / 2, screenY_signal_top + signalDiameter / 2 + fm.getAscent() / 2 - 1);
    }

    private void drawCar(Graphics2D g2d, Car car, int roadVisualTopY, int laneVisualHeight) {
        int panelWidth = getWidth();
        int carScreenX = (int) ((car.getPosition() / road.getLength()) * panelWidth) - CAR_RENDER_WIDTH / 2;
        int globalLaneForDrawing = road.getGlobalLaneIndexForDrawing(car.getCurrentLaneIndex(), car.getDirection());
        if (globalLaneForDrawing == -1) return;

        double yOffsetForLaneChange = 0;
        double rotationForLaneChange = 0;

        if (car.isChangingLane()) {
            int globalTargetLaneForDrawing = road.getGlobalLaneIndexForDrawing(car.getTargetLaneForChange(), car.getDirection());
            if (globalTargetLaneForDrawing != -1) {
                double progress = car.getLaneChangeProgress();
                yOffsetForLaneChange = (globalTargetLaneForDrawing - globalLaneForDrawing) * laneVisualHeight * progress;
                double normalizedProgress = (progress < 0.5) ? (progress * 2.0) : ((1.0 - progress) * 2.0);
                int changeDirectionSign = (globalTargetLaneForDrawing > globalLaneForDrawing) ? 1 : -1;
                rotationForLaneChange = changeDirectionSign * MAX_CAR_TILT_ANGLE * Math.sin(normalizedProgress * Math.PI);
            }
        }

        int carCenterYOnLane = roadVisualTopY + (globalLaneForDrawing * laneVisualHeight) + laneVisualHeight / 2;
        int carTopY = carCenterYOnLane - CAR_RENDER_HEIGHT / 2 + (int)yOffsetForLaneChange;

        AffineTransform oldTransform = g2d.getTransform();
        g2d.rotate(rotationForLaneChange, carScreenX + CAR_RENDER_WIDTH / 2.0, carTopY + CAR_RENDER_HEIGHT / 2.0);

        g2d.setColor(WHEEL_COLOR);
        int upperWheelY = carTopY + WHEEL_OFFSET_Y;
        int lowerWheelY = carTopY + CAR_RENDER_HEIGHT - WHEEL_DIAMETER - WHEEL_OFFSET_Y;
        int frontWheelX = carScreenX + WHEEL_OFFSET_X;
        int rearWheelX = carScreenX + CAR_RENDER_WIDTH - WHEEL_OFFSET_X - WHEEL_DIAMETER;
        g2d.fill(new Ellipse2D.Double(frontWheelX, upperWheelY, WHEEL_DIAMETER, WHEEL_DIAMETER));
        g2d.fill(new Ellipse2D.Double(rearWheelX, upperWheelY, WHEEL_DIAMETER, WHEEL_DIAMETER));
        g2d.fill(new Ellipse2D.Double(frontWheelX, lowerWheelY, WHEEL_DIAMETER, WHEEL_DIAMETER));
        g2d.fill(new Ellipse2D.Double(rearWheelX, lowerWheelY, WHEEL_DIAMETER, WHEEL_DIAMETER));

        Color carBodyColor = (car.getDirection() == 1) ? new Color(50, 90, 180) : new Color(180, 50, 90);
        Shape carShape = new RoundRectangle2D.Double(carScreenX, carTopY, CAR_RENDER_WIDTH, CAR_RENDER_HEIGHT, CAR_ARC_RADIUS, CAR_ARC_RADIUS);
        g2d.setColor(carBodyColor);
        g2d.fill(carShape);

        if (car.isBraking()) {
            g2d.setColor(BRAKE_LIGHT_COLOR);
            int brakeLightHeight = BRAKE_LIGHT_SIZE;
            int brakeLightWidth = BRAKE_LIGHT_SIZE / 2;
            int brakeLightY = carTopY + (CAR_RENDER_HEIGHT - brakeLightHeight) / 2;

            if (car.getDirection() == 0) {
                g2d.fillRect(carScreenX , brakeLightY, brakeLightWidth, brakeLightHeight);
                g2d.fillRect(carScreenX + CAR_RENDER_WIDTH - brakeLightWidth*2 - 2 , brakeLightY, brakeLightWidth, brakeLightHeight);
            } else {
                g2d.fillRect(carScreenX + CAR_RENDER_WIDTH - brakeLightWidth , brakeLightY, brakeLightWidth, brakeLightHeight);
                g2d.fillRect(carScreenX + brakeLightWidth/2 + 2, brakeLightY, brakeLightWidth, brakeLightHeight);
            }
        }

        g2d.setColor(CAR_WINDOW_COLOR);
        int windowWidth = CAR_RENDER_WIDTH / 2;
        int windowHeight = CAR_RENDER_HEIGHT / 2 - 2;
        int windowXOffset = (car.getDirection() == 0) ? CAR_RENDER_WIDTH / 2 - 2 : 2;
        g2d.fillRoundRect(carScreenX + windowXOffset, carTopY + 2, windowWidth, windowHeight, CAR_ARC_RADIUS / 2, CAR_ARC_RADIUS / 2);

        g2d.setColor(carBodyColor.darker());
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(carShape);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
        String speedText = String.format("%.0f", car.getCurrentSpeed() * 3.6);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(speedText, carScreenX + CAR_RENDER_WIDTH / 2 - fm.stringWidth(speedText) / 2, carTopY + CAR_RENDER_HEIGHT / 2 + fm.getAscent() / 2 - 2);

        g2d.setTransform(oldTransform);
    }

    private void drawInfoPanel(Graphics2D g2d) {
        g2d.setColor(new Color(0,0,0,150));
        g2d.fillRect(5,5, 250, 50);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString(String.format("Время: %.1f c", simulationTime), 10, 20);
        g2d.drawString("Машин: " + (road.getCars() != null ? road.getCars().size() : 0), 10, 35);
        String roadInfo = "Дорога: " + road.getType() + ", " +
                (road.getNumberOfDirections() == 1 ? "1-стор." : "2-стор.") + ", " +
                road.getLanesPerDirection() + " п./напр.";
        if(road.getType() == RoadType.TUNNEL) roadInfo = "Дорога: " + road.getType() + " (реверс.)";
        g2d.drawString(roadInfo, 10, 50);
    }

    private void drawPlacementHint(Graphics2D g2d, int currentRoadRenderHeight) {
        if (placementModeActive && placementHint != null && !placementHint.isEmpty()) {
            g2d.setColor(new Color(0,0,200, 200));
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            String text = placementHint;

            FontMetrics fm = g2d.getFontMetrics();
            int w = fm.stringWidth(text);
            int hintY = getHeight()/2 + currentRoadRenderHeight/2 + 30;
            if (hintY + fm.getHeight() > getHeight() -10) {
                hintY = getHeight() - 20 - fm.getDescent();
            }
            if (hintY - fm.getAscent() < getHeight()/2 + currentRoadRenderHeight/2 + 5 &&
                    getHeight()/2 - currentRoadRenderHeight/2 - 30 > 20) {
                hintY = getHeight()/2 - currentRoadRenderHeight/2 - 15 - fm.getHeight();
            }

            g2d.fillRect(getWidth() / 2 - w / 2 - 10, hintY - fm.getAscent(), w + 20, fm.getHeight() + 5);
            g2d.setColor(Color.WHITE);
            g2d.drawString(text, getWidth() / 2 - w / 2, hintY);
        }
    }

    private void drawRoadSign(Graphics2D g2d, RoadSign sign, int screenX, int screenY_sign_top, int roadVisualTopY, int currentRoadRenderHeight, boolean isAboveRoad) {
        g2d.setColor(ROAD_SIGN_POLE_COLOR);
        int poleX = screenX + ROAD_SIGN_SIZE / 2 - 2;
        int poleBaseY = isAboveRoad ? (roadVisualTopY - SHOULDER_WIDTH - OBJECT_SIDE_OFFSET) : (roadVisualTopY + currentRoadRenderHeight + SHOULDER_WIDTH + OBJECT_SIDE_OFFSET);
        int poleTopY = isAboveRoad ? (poleBaseY - ROAD_SIGN_POLE_HEIGHT) : poleBaseY;
        int poleRenderHeight = ROAD_SIGN_POLE_HEIGHT;

        if(isAboveRoad) g2d.fillRect(poleX, poleTopY, 4, poleRenderHeight);
        else g2d.fillRect(poleX, poleBaseY, 4, poleRenderHeight);

        if (sign.getType() == RoadSignType.SPEED_LIMIT) {
            g2d.setColor(ROAD_SIGN_BG_COLOR);
            g2d.fillOval(screenX, screenY_sign_top, ROAD_SIGN_SIZE, ROAD_SIGN_SIZE);
            g2d.setColor(ROAD_SIGN_BORDER_COLOR);
            g2d.setStroke(new BasicStroke(Math.max(1, ROAD_SIGN_SIZE / 10f)));
            g2d.drawOval(screenX, screenY_sign_top, ROAD_SIGN_SIZE, ROAD_SIGN_SIZE);
            g2d.setStroke(new BasicStroke(1));
            g2d.setColor(ROAD_SIGN_TEXT_COLOR);
            String text = String.format("%.0f", sign.getSpeedLimitKmh());
            int fontSize = (sign.getSpeedLimitKmh() >= 100) ? (int)(ROAD_SIGN_SIZE / 2.3f) : ((sign.getSpeedLimitKmh() < 10) ? (int)(ROAD_SIGN_SIZE / 1.9f) : ROAD_SIGN_SIZE / 2);
            g2d.setFont(new Font("Arial", Font.BOLD, fontSize));
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(text, screenX + ROAD_SIGN_SIZE / 2 - fm.stringWidth(text) / 2, screenY_sign_top + ROAD_SIGN_SIZE / 2 + fm.getAscent() / 2 - fm.getDescent() / 3);
        } else {
            g2d.setColor(Color.BLUE); g2d.fillRect(screenX, screenY_sign_top, ROAD_SIGN_SIZE, ROAD_SIGN_SIZE);
            g2d.setColor(Color.WHITE); g2d.setFont(new Font("Arial", Font.BOLD, ROAD_SIGN_SIZE - 8));
            g2d.drawString("?", screenX + ROAD_SIGN_SIZE/2 - g2d.getFontMetrics().stringWidth("?")/2, screenY_sign_top + ROAD_SIGN_SIZE/2 + g2d.getFontMetrics().getAscent()/2 - g2d.getFontMetrics().getDescent()/2);
        }
    }
}