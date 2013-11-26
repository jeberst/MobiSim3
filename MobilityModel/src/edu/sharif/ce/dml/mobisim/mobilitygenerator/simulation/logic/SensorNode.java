/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.logic;

/**
 *
 * @author Josh
 */

import edu.sharif.ce.dml.common.data.entity.DataLocation;
import edu.sharif.ce.dml.common.logic.entity.Location;


public class SensorNode extends GeneratorNode {
    public  SensorNode topnode;
    public SensorNode bottomnode;
    public SensorNode leftnode;
    public SensorNode rightnode;
    private boolean center;
    public int sensorrange;
    public GeneratorNode defaultnode;
    public int coverage;
    public int idealcoverage;
    
    public boolean bridgeNode;
    public boolean bridgeDependent;
    
    //These fields will be used for Distributed BFS
    int idealposition = 0;
    
    public SensorNode(GeneratorNode defaultnode, boolean center, SensorNode top,  SensorNode right, SensorNode bottom,  SensorNode left)
    {
        this.defaultnode = defaultnode;
        this.center = center;
        this.bottomnode = bottom;
        this.topnode = top;
        this.rightnode = right;
        this.leftnode = left;
    }
    public SensorNode(GeneratorNode defaultnode, boolean center)
    {
        this.defaultnode = defaultnode;
        this.center = center;
    }
    public SensorNode()
    {
        this.setSpeed(0);

    }
    public SensorNode(String name)
    {
        this.setName(name);
        this.location = new DataLocation(200,200);
        this.direction = 0;
        this.speed=0;
        this.range = 0;
    }
    
    public SensorNode(GeneratorNode node)
    {
        this.defaultnode = node;
        this.setName("" + node.getName());
        this.location = node.getLocation();
        this.direction = node.getDirection();
        this.speed = node.getSpeed();
        this.range = node.getRange();
    }

    
    public boolean isCenter(SensorNode node)
    {
        return node.center;
    }
    public boolean isCenter()
    {
        return center;
    }
    
    public void setCenter()
    {
        center = true;
    }
    
    public void setCoverage(int c)
    {
        this.coverage = c;
    }
    
    public int getCoverage()
    {
        return this.coverage;
    }
    
    public void setSpeed(int speed)
    {
        this.defaultnode.setSpeed(speed);
    }
    
    //Public setCenter
    //Public Set Node(s)
    
}
