/*
 * Copyright (c) 2005-2008 by Masoud Moshref Javadi <moshref@ce.sharif.edu>, http://ce.sharif.edu/~moshref
 * The license.txt file describes the conditions under which this software may be distributed.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.model.group;

import edu.sharif.ce.dml.common.data.entity.DataLocation;
import edu.sharif.ce.dml.common.logic.entity.Location;
import edu.sharif.ce.dml.common.parameters.logic.Parameter;
import edu.sharif.ce.dml.common.parameters.logic.complex.LazySelectOneParameterable;
import edu.sharif.ce.dml.common.parameters.logic.exception.InvalidParameterInputException;
import edu.sharif.ce.dml.common.parameters.logic.primitives.*;
import edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.logic.GeneratorNode;
import edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.logic.SensorNode;
import edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.logic.Simulation;
import edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.model.MapHandleSupport;
import edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.model.Model;
import edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.model.Model.NodePainter;
import edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.model.ModelInitializationException;
import edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.model.SubModelException;
import edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.model.maps.IncludableMap;
import edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.model.maps.IncludingMap;
import edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.model.maps.ReflectiveMap;
import edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.model.poweralgorithm.PowerAlgorithm;
import edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.model.randomwalk.RandomWalkModel;
import edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.ui.map.MapHandle;
import edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.ui.map.MapHandleGroup;
import edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.ui.map.multi.MultiMapHandleGroup;

import java.awt.*;
import java.util.*;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: Masoud
 * Date: Oct 28, 2006
 * Time: 9:16:51 PM
 * <br/>this implementation is with hypothosis that all timeSteps are equal (because of generating nextDestNode of
 * member nodes according to random speed and angle deviation from leader.
 * as leaders mobility model is {@link RandomWalkModel} so we have not any pause time
 * <br/> list of uiparameters: <br/>
 * {@link Integer} maxspeed<br/>
 * {@link Integer} minspeed<br/>
 * {@link Integer} maxpausetime: in this implementation it is not used it.<br/>
 * {@link Double} adr: a double number 1>= adr >=0 <br/>
 * {@link Double} sdr: a double number 1>= sdr >=0<br/>
 * {@link Integer} maxinitialdistance: maximum initial distance of group nodes from group leader<br/>
 * {@link edu.sharif.ce.dml.common.parameters.logic.primitives.IntegerArrayParameter} groupsizes:
 * shows the distributions of nodes into the groups by describing
 * size of each group as a comma separated String. what does happen if number of nodes is not equal to
 * sum of groups size see {@link edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.model.Model#setModelNodes(java.util.List < edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.logic.GeneratorNode >)}  <br/>
 */

//{@link Double} groupmaxspeed: speed deviation of groups members from group leader will be:
// * <tt>groupmaxspeed*adr*randomNumber</tt><br/>
// * {@link Double} groupmaxangle: angle deviation of groups members from group leader will be:
// * <tt>groupmaxangle*adr*randomNumber</tt><br/>
public abstract class AbstractGroupModel extends Model implements IncludableMap, MapHandleSupport {
    protected static final int MAX_STACK_DEPTH = 50;
    protected final Map<NodeInGroup, Group> leaderGroup = new HashMap<NodeInGroup, Group>();
    protected final Map<GeneratorNode, NodeInGroup> nodeNodeInGroup = new TreeMap<GeneratorNode, NodeInGroup>();
    protected final String[] traceLabels = new String[]{"GroupNumber", "Is Leader"};
    protected LazySelectOneParameterable leadersModel = null;
    protected Location leadersModelOffset = new Location(0, 0);
    protected int maxInitialDistance;
    protected double maxAngle = 2 * Math.PI;
    protected double ADR;
    protected double SDR;
    protected boolean extraTraces = true;
    protected boolean perGroupRange = false;
    protected List<Integer> groupMembersNum;
    protected int stackDepth = 0;
    protected List<SensorNode> sensors = new LinkedList<>();
    private HashSet coverednodes = new HashSet();
    
    private List<SensorNode> usedSensors = new LinkedList<>();
    List<SensorNode> visited = new LinkedList<>();
    
    
            


    public AbstractGroupModel() {
        super();

        ADR = 0;
        SDR = 0;
        maxInitialDistance = 0;
        groupMembersNum = new LinkedList<Integer>();
        leadersModel = new LazySelectOneParameterable();
        perGroupRange = false;
      
    }
    
                  
           

    public Map<String, Parameter> getParameters() {
        Map<String, Parameter> parameters = super.getParameters();
        parameters.put("extratraces", new BooleanParameter("extratraces", extraTraces));
        parameters.put("pergrouprange", new BooleanParameter("pergrouprange", perGroupRange));

        parameters.put("leadersmodel", leadersModel);
        parameters.put("adr", new DoubleParameter("adr", ADR));
        parameters.put("sdr", new DoubleParameter("sdr", SDR));
        parameters.put("maxinitialdistance", new IntegerParameter("maxinitialdistance", maxInitialDistance));
        parameters.put("groupsizes", new IntegerArrayParameter
                ("groupsizes", groupMembersNum.toArray(new Integer[groupMembersNum.size()])));
        parameters.put("leadersmodeloffset", new DoubleArrayParameter
                ("leadersmodeloffset", new double[]{leadersModelOffset.getX(), leadersModelOffset.getY()}));
        return parameters;
    }

    public void setParameters(Map<String, Parameter> parameters) throws InvalidParameterInputException {
        super.setParameters(parameters);

        extraTraces = (Boolean) parameters.get("extratraces").getValue();
        perGroupRange = (Boolean) parameters.get("pergrouprange").getValue();

//        maxAngle = Double.parseDouble(uiparameters.get("groupmaxangle"));
        ADR = (Double) parameters.get("adr").getValue();
        SDR = (Double) parameters.get("sdr").getValue();
        maxInitialDistance = (Integer) parameters.get("maxinitialdistance").getValue();
//        if (leadersModel != null) {
//            leadersModel.setParameters(parameters);
//        }
        leadersModel = (LazySelectOneParameterable) parameters.get("leadersmodel");
        int[] tempGroupSizes = (int[]) parameters.get("groupsizes").getValue();

        groupMembersNum = new LinkedList<Integer>();
        for (Integer gsize : tempGroupSizes) {
            groupMembersNum.add(gsize);
        }
        double[] iap = (double[]) ((DoubleArrayParameter) parameters.get("leadersmodeloffset")).getValue();
        leadersModelOffset.setX(iap[0]);
        leadersModelOffset.setY(iap[1]);

    }

    /**
     * {@inheritDoc}
     * <br/>if the sum of group sizes is not equal to <code>modelNodes.size()</code>
     * nodes will be distributed equally on groups and additional nodes will be set in last group.
     * if groupsizes indicates more groups than
     * <code>modelNodes.size</code> model creates <code>modelNodes.size</code> groups,
     * each group will have <code>size=1</code>
     *
     * @param modelNodes
     */
    public void setModelNodes(List<GeneratorNode> modelNodes) throws ModelInitializationException {
       // System.out.println("Abstract group " + modelNodes.size());
        super.setModelNodes(modelNodes);
          
        //calculating group members
        if (sumGroupNodes() != (modelNodes.size())) {
            int numberOfGroups = Math.min(groupMembersNum.size(), modelNodes.size());//number of groups at most can be equal to number of nodes.
            groupMembersNum = new LinkedList<Integer>();
            int currentGroupSize = (modelNodes.size()) / numberOfGroups;
            for (int i = 0; i < numberOfGroups - 1; i++) {
                groupMembersNum.add(currentGroupSize);
            }
            groupMembersNum.add(modelNodes.size() - groupMembersNum.size() * currentGroupSize);
        }

        //create group structures
        nodeNodeInGroup.clear();
        leaderGroup.clear();
        Iterator<GeneratorNode> itr = modelNodes.iterator();
        int groupId = 0;
        List<GeneratorNode> leadersNode = new LinkedList<GeneratorNode>();
        for (Integer groupSize : groupMembersNum) {
            if (groupSize > 0) {
                NodeInGroup groupLeader = new NodeInGroup(itr.next(), NodeInGroup.GROUP_LEADER, null);
                Group group = new Group(groupLeader, groupId++);
                groupLeader.setGroup(group);
                group.setNodes(new LinkedList<NodeInGroup>());
                group.getNodes().add(groupLeader);
                leaderGroup.put(groupLeader, group);
                leadersNode.add(groupLeader.getNode());
                nodeNodeInGroup.put(groupLeader.getNode(), groupLeader);
                for (int i = 1; i < groupSize; i++) {
                    NodeInGroup member = createNodeInGroup(itr.next(), NodeInGroup.GROUP_MEMBER, group);
                    group.getNodes().add(member);
                    nodeNodeInGroup.put(member.getNode(), member);
                }
            }
        }
        getLeadersModel().setModelNodes(leadersNode);
        nodePainter = new GroupMemberNodePainter(getLeadersModel().getNodePainter());
    }

    protected Model getLeadersModel() {
        return (Model) leadersModel.getValue();
    }

    protected NodeInGroup createNodeInGroup(GeneratorNode node, String groupMemeber, Group group) {
        return new NodeInGroup(node, groupMemeber, group);
    }

    public MapHandleSupport getMapHandleSupport() {
        return this;
    }

    /**
     * @param leaderNode
     * @return new random speed of a node in a group with <code>leaderGroup</code> leader.
     *         between leadergroup-maxGroupSpeed
     */
    protected double generateSpeed(GeneratorNode leaderNode) {
        double leaderSpeed = leaderNode.getSpeed();
        double speedDev = ((getRandomValue() * SDR * 2) - SDR) * leaderSpeed;
        return leaderSpeed + speedDev;
    }

    /**
     * overridden to make group by group update location for nodes. may be it can have a better implementation.
     *
     * @param timeStep
     */
    public void updateNodes(double timeStep) {
        super.setNodeCoverage(0);
        coverednodes.clear();
        usedSensors.clear();
      
        HashSet<GeneratorNode> localCoveredNodes = new HashSet<GeneratorNode>();
       
        getLeadersModel().updateNodes(timeStep);
        //for each group (by each group leader)
        for (NodeInGroup groupLeader : leaderGroup.keySet()) {
            usedSensors.clear();
            //update group leader
            //getLeadersModel().updateLoc(timeStep, groupLeader.getNode());
            //update group's other nodes
            for (NodeInGroup anyNodeInGroup : leaderGroup.get(groupLeader).getNodes()) {
                if (!anyNodeInGroup.isLeader()) {
                    getNextStep(timeStep, anyNodeInGroup.getNode());
                }
                if(anyNodeInGroup.node.isSensorNode())
                {
                  /*  if(anyNodeInGroup.node.getIntName()==5)
                    {
                    /*anyNodeInGroup.node.setSpeed(0);
                    DataLocation L = new DataLocation(200, 200);
                    anyNodeInGroup.node.setLocation(L);
                      anyNodeInGroup.node.setSpeed(1);
                    getNextSensorStep(timeStep, anyNodeInGroup.node);
                    int sensor1coverage = pollSensor(modelNodes, anyNodeInGroup.node);
                    //super.setNodeCoverage(super.getCoverage() + sensor1coverage);
                    Model.nodecoverage += sensor1coverage;
                          Simulation.writecoverage(anyNodeInGroup.node.getName() + " Nodes covered: " + sensor1coverage + "\r\n");
                    }
                    else
                    {
                        int coverage = pollSensor(modelNodes, anyNodeInGroup.node);
                        //super.setNodeCoverage(super.getCoverage() + coverage);
                        Model.nodecoverage += coverage;
                        //write("Node count for time: " + timeStep +  " = " + x);
                            Simulation.writecoverage(anyNodeInGroup.node.getName() + " Nodes covered: " + coverage + "\r\n");
                    }*/
                    SensorNode s = new SensorNode(anyNodeInGroup.node);
                    //anyNodeInGroup.node.setSpeed(1);
                    s.setSpeed(3);

                    getNextSensorStep(timeStep, anyNodeInGroup.node, localCoveredNodes);
                     s.coverage = pollSensor(modelNodes, anyNodeInGroup.node);
                        Model.nodecoverage += s.coverage;
                        //write("Node count for time: " + timeStep +  " = " + x);
                            Simulation.writecoverage(anyNodeInGroup.node.getName() + "\t" + s.coverage + "\r\n");

                }
            }
            
        }
        
        
             Simulation.writecoverage( "Total Nodes covered: " + Model.nodecoverage + "\r\n");
        updateRanges();
    }
    
    
 public Location positionSensors(SensorNode sensor, HashSet coveredNodes, List<GeneratorNode> allNodes)
 {
    
     List<GeneratorNode> uncoveredNodes = new LinkedList<GeneratorNode>();
     uncoveredNodes.clear();
     DataLocation newSensorLocation =   newSensorLocation = new DataLocation(200, 200);;
     int x = 0;
     int y = 0;

     
        for(GeneratorNode node : allNodes)
            {
                if(!coveredNodes.contains(node) && !node.isSensorNode() && !uncoveredNodes.contains(node))
                {
                    uncoveredNodes.add(node);
                }
            }
         //These Location Calculations are causing issues, not sure what is wrong.
         int x2 = sensor.defaultnode.getLocation().getX();
         int y2 = sensor.defaultnode.getLocation().getY();
         
         
         if(!uncoveredNodes.isEmpty())
         {
                      x = calculateAverageCenter(uncoveredNodes).getX();
                       y = calculateAverageCenter(uncoveredNodes).getY();
                       
                       int j= allNodes.size()-super.GetSensorCount();
             //Place the first node.
            if(uncoveredNodes.size() == j)
            {
               newSensorLocation = calculateAverageCenter(uncoveredNodes);
               sensor.idealcoverage = pollLocalSensor(allNodes, sensor.defaultnode, coveredNodes);
               this.usedSensors.add(sensor);
               sensor.setCenter();
            }
            else
               {
                   Random rand = new Random(); 
                    int value = rand.nextInt(this.usedSensors.size()); 
                           //This needs to be changed to support searching through the nodes.
                           //newSensorLocation = BreadthFirstSearch(uncoveredNodes, this.usedSensors.get(value), sensor);
                           visited.clear();
                           newSensorLocation = DistributedBreadthFirstSearch(uncoveredNodes, this.usedSensors.get(0), sensor).candidateLocation;
                           //newSensorLocation = new DataLocation(usedsensor.defaultnode.getLocation().getX(), usedsensor.defaultnode.getLocation().getY()+25);
                           sensor.idealcoverage = pollLocalSensor(allNodes, sensor.defaultnode, coveredNodes); 
                           int localcoverage = sensor.idealcoverage;
                           this.usedSensors.add(sensor);
               }
         }
         else
         {
         newSensorLocation = usedSensors.get(0).defaultnode.getLocation();
         }
         
         //need an else here to figure out what to do when all the nodes are covered.
         
     return new Location(newSensorLocation);
 }
 
 public List<GeneratorNode> getNonSensorNodes() {
     List<GeneratorNode> modelNodeList = new ArrayList<GeneratorNode>();
     for (GeneratorNode node : this.modelNodes) {
         if (!node.isSensorNode()) {
             modelNodeList.add(node);
         }
     }
     return modelNodeList;
 }
 
 public GeneratorNode[] getNonSensorNodesArray() {
     List<GeneratorNode> modelNodeList = getNonSensorNodes();
     return modelNodeList.toArray(new GeneratorNode[modelNodeList.size()]);
 }

 /**
  * Single-link clustering algorithm
  * Reference http://www.cs.princeton.edu/courses/archive/spring10/cos233/lectures/cos233-234-lecture7.pdf for algorithm description
  * @return a list of Cluster objects
  */ 
 public List<Cluster> getClusters() {
     List<Cluster> clusters = new ArrayList<Cluster>();
     GeneratorNode[] nonSensorNodes = getNonSensorNodesArray();
     double INFINITY = Double.POSITIVE_INFINITY;
     
     // compute distance between all nodes
     double distance[][] = new double[nonSensorNodes.length][nonSensorNodes.length];
     int distanceMinimum[] = new int[nonSensorNodes.length];
     for (int i = 0; i < nonSensorNodes.length; i++) {
         for (int j = 0; j < nonSensorNodes.length; j++) {
             if (i == j) {
                 distance[i][j] = INFINITY;
             }
             else {
                 distance[i][j] = calcDistance(nonSensorNodes[i], nonSensorNodes[j]);
             }
             if (distance[i][j] < distance[i][distanceMinimum[i]]) {
                 distanceMinimum[i] = j;
             }
         }
     }
     
     // single-link clustering main loop
     for (int s = 0; s < (nonSensorNodes.length - 1); s++) {
         // find closest pair of clusters (i1, i2)
         int i1 = 0;
         for (int i = 0; i < nonSensorNodes.length; i++) {
             if (distance[i][distanceMinimum[i]] < distance[i1][distanceMinimum[i1]]) {
                 i1 = i;
             }
         }
         int i2 = distanceMinimum[i1];
         
         // overwrite row i1 with minimum of entries in row i1 and i2
         for (int j = 0; j < nonSensorNodes.length; j++) {
             if (distance[i2][j] < distance[i1][j]) {
                 distance[i1][j] = distance[j][i1] = distance[i2][j];
             }
         }
         distance[i1][i1] = INFINITY;
         
         // infinity-out old row i1 and column i2
         for (int i = 0; i < nonSensorNodes.length; i++) {
             distance[i2][i] = distance[i][i2] = INFINITY;
         }
     }
     
     return clusters;
 }
 
  /**
  * @return an array of Cluster objects
  */ 
 public Cluster[] getClustersArray() {
     List<Cluster> clusters = getClusters();
     return clusters.toArray(new Cluster[clusters.size()]);
 }
 
 public Cluster getBiggestCluster() {
     Cluster biggestCluster = null;
     int biggestClusterSize = 0;
     for (Cluster cluster : getClusters()) {
         if (cluster.getClusterSize() > biggestClusterSize) {
             biggestClusterSize = cluster.getClusterSize();
             biggestCluster = cluster;
         }
     }
     return biggestCluster;
 }
 
 
 public DataLocation BreadthFirstSearch(List<GeneratorNode> nodes, SensorNode rootSensor, SensorNode sensor)
 {
     Queue<SensorNode> sensorqueue = new LinkedList<>();
    List<SensorNode> BFSvisited = new LinkedList<>();
     sensorqueue.add(rootSensor);
     int bestcoverage = 0;
     SensorNode s;
     SensorNode basenode = rootSensor;
     int position = 0;
     
     int debugi = 0;
     
     while(!sensorqueue.isEmpty())
     {
         debugi++;
         if(debugi > 1000)
         {
             //Caused an infinite loop
             debugi =0;
         }
         
         s = sensorqueue.remove();
         BFSvisited.add(s);
        
         if(s.topnode != null && s.topnode != rootSensor && s.topnode != sensor && !sensorqueue.contains(s.topnode) && !BFSvisited.contains(s.topnode) )
         {
             sensorqueue.add(s.topnode);
         }
         else
         {
             int poll = pollPosition(nodes, new DataLocation(s.defaultnode.getLocation().getX(), s.defaultnode.getLocation().getY()+25));
             if( poll > bestcoverage)
             {
                 bestcoverage = poll;
                 basenode = s;
                 position = 1;
             }
         }
         if(s.rightnode != null && s.rightnode != rootSensor && s.rightnode != sensor && !sensorqueue.contains(s.rightnode) && !BFSvisited.contains(s.rightnode))
         {
             sensorqueue.add(s.rightnode);
         }
         else
         {
             int poll = pollPosition(nodes, new DataLocation(s.defaultnode.getLocation().getX()+25, s.defaultnode.getLocation().getY()));
             if( poll > bestcoverage)
             {
                 bestcoverage = poll;
                 basenode = s;
                 position = 2;
             }
         }
         if(s.bottomnode != null && s.bottomnode != rootSensor && s.bottomnode != sensor && !sensorqueue.contains(s.bottomnode) && !BFSvisited.contains(s.bottomnode))
         {
              sensorqueue.add(s.bottomnode);
         }
          else
         {
             int poll = pollPosition(nodes, new DataLocation(s.defaultnode.getLocation().getX(), s.defaultnode.getLocation().getY()-25));
             if( poll > bestcoverage)
             {
                 bestcoverage = poll;
                 basenode = s;
                 position = 3;
             }
         }
         if(s.leftnode != null && s.leftnode != rootSensor  && s.leftnode != sensor && !sensorqueue.contains(s.leftnode) && !BFSvisited.contains(s.leftnode))
         {
             sensorqueue.add(s.leftnode);
         }
         else
         {
             int poll = pollPosition(nodes, new DataLocation(s.defaultnode.getLocation().getX()-25, s.defaultnode.getLocation().getY()));
             if( poll > bestcoverage)
             {
                 bestcoverage = poll;
                 basenode = s;
                 position = 4;
             }
         }
     }
     
     
     if(position == 1)
     {
         basenode.topnode = sensor;
         sensor.bottomnode = basenode;
         return new DataLocation(basenode.defaultnode.getLocation().getX(), basenode.defaultnode.getLocation().getY()+25);
     }
     else if(position == 2)
     {
         basenode.rightnode = sensor;
         sensor.leftnode = basenode;
         return new DataLocation(basenode.defaultnode.getLocation().getX()+25, basenode.defaultnode.getLocation().getY());
     }
      else if(position == 3)
     {
         basenode.bottomnode = sensor;
         sensor.topnode = basenode;
         return new DataLocation(basenode.defaultnode.getLocation().getX(), basenode.defaultnode.getLocation().getY()-25);
     }
      else if(position == 4)
     {
         basenode.leftnode = sensor;
         sensor.rightnode = basenode;
         return new DataLocation(basenode.defaultnode.getLocation().getX()-25, basenode.defaultnode.getLocation().getY());
     }
     
     //This is where we should input the logic for moving sensors with no values to bridge the gap between nodes that we can reach.
     //return new DataLocation(0,0);
     return rootSensor.defaultnode.getLocation();
 }
 
 
 public CandidateLocation DistributedBreadthFirstSearch(List<GeneratorNode> nodes, SensorNode rootSensor, SensorNode sensor)
 {
     Queue<SensorNode> sensorqueue = new LinkedList<>();
     sensorqueue.add(rootSensor);
     int bestcoverage = 0;
     SensorNode s;
     SensorNode basenode = rootSensor;
     int position = 0;
     List<CandidateLocation> candidateLocations = new LinkedList<>();
      CandidateLocation bestLocation;
      bestLocation = new CandidateLocation(0, rootSensor, 0, rootSensor.defaultnode.getLocation());
     
      int debugi = 0;
     
         debugi++;
         if(debugi > 1000)
         {
             //Caused an infinite loop
             debugi =0;
         }
         
         s = rootSensor;
         visited.add(s);
        
         if(s.topnode != null && !visited.contains(s.topnode) && s != sensor )
         {
            candidateLocations.add(DistributedBreadthFirstSearch(nodes, s.topnode, sensor));
         }
         else
         {
             
             int poll = pollPosition(nodes, new DataLocation(s.defaultnode.getLocation().getX(), s.defaultnode.getLocation().getY()+25));
             if( poll > bestcoverage)
             {
                 bestcoverage = poll;
                 basenode = s;
                 position = 1;
                 bestLocation = new CandidateLocation(poll, s, 1, new DataLocation(s.defaultnode.getLocation().getX(), s.defaultnode.getLocation().getY()+25));
                 candidateLocations.add(bestLocation);
             }
         }
         if(s.rightnode != null && !visited.contains(s.rightnode) && s != sensor)
         {
             candidateLocations.add(DistributedBreadthFirstSearch(nodes,s.rightnode,sensor));
         }
         else
         {
             int poll = pollPosition(nodes, new DataLocation(s.defaultnode.getLocation().getX()+25, s.defaultnode.getLocation().getY()));
             if( poll > bestcoverage)
             {
                 bestcoverage = poll;
                 basenode = s;
                 position = 2;
                 bestLocation = new CandidateLocation(poll, s, 2, new DataLocation(s.defaultnode.getLocation().getX()+25, s.defaultnode.getLocation().getY()));
                 candidateLocations.add(bestLocation);
             }
         }
         if(s.bottomnode != null && !visited.contains(s.bottomnode) && s != sensor)
         {
                candidateLocations.add(DistributedBreadthFirstSearch(nodes,s.bottomnode,sensor));
         }
          else
         {
             int poll = pollPosition(nodes, new DataLocation(s.defaultnode.getLocation().getX(), s.defaultnode.getLocation().getY()-25));
             if( poll > bestcoverage)
             {
                 bestcoverage = poll;
                 basenode = s;
                 position = 3;
                 bestLocation = new CandidateLocation(poll, s, 3, new DataLocation(s.defaultnode.getLocation().getX(), s.defaultnode.getLocation().getY()-25));
                 candidateLocations.add(bestLocation);
             }
         }
         if(s.leftnode != null && !visited.contains(s.leftnode) && s != sensor)
         {
             candidateLocations.add(DistributedBreadthFirstSearch(nodes,s.leftnode,sensor));
         }
         else
         {
             int poll = pollPosition(nodes, new DataLocation(s.defaultnode.getLocation().getX()-25, s.defaultnode.getLocation().getY()));
             if( poll > bestcoverage)
             {
                 bestcoverage = poll;
                 basenode = s;
                 position = 4;
                 bestLocation = new CandidateLocation(poll, s, 4, new DataLocation(s.defaultnode.getLocation().getX()-25, s.defaultnode.getLocation().getY()));
                 candidateLocations.add(bestLocation);
             }
         }
     
     
     for(CandidateLocation newLocation : candidateLocations)
     {
       
         if(newLocation.coverage > bestLocation.coverage)
         {
             bestLocation = newLocation;
         }
     }
     
 
      
     if(bestLocation.position == 1)
     {
         bestLocation.basenode.topnode = sensor;
         sensor.bottomnode = bestLocation.basenode;
         return bestLocation;
     }
     else if(bestLocation.position == 2)
     {
         bestLocation.basenode.rightnode = sensor;
         sensor.leftnode = bestLocation.basenode;
         return bestLocation; 
     }
      else if(bestLocation.position == 3)
     {
         bestLocation.basenode.bottomnode = sensor;
         sensor.topnode = bestLocation.basenode;
         return bestLocation;
     }
      else if(bestLocation.position == 4)
     {
         bestLocation.basenode.leftnode = sensor;
         sensor.rightnode = bestLocation.basenode;
         return bestLocation; 
     }
     
     
     //This is where we should input the logic for moving sensors with no values to bridge the gap between nodes that we can reach.
     //return new DataLocation(0,0);
     return bestLocation;
 }
 
 public double canLinkDistance(List<SensorNode> sensorNodes)
 {
     return super.getSensorRange();
 }
         public int pollSensor(List<GeneratorNode> nodes, GeneratorNode sensor)
        {
            //System.out.println("Calculated Sensor Range " + super.getSensorRange());
            int nodesinrange = 0;
           for(GeneratorNode node : nodes)
           {
               //("Distance " + node.getName() + "=" + calcDistance(node, sensor));
              if(calcDistance(node, sensor) < super.getSensorRange() && !node.isSensorNode() && !coverednodes.contains(node))
              {
                  nodesinrange++;
                  coverednodes.add(node);
              }
           }
            return nodesinrange;
        }
         
           public int pollLocalSensor(List<GeneratorNode> nodes, GeneratorNode sensor, HashSet localCoveredNodes)
        {
            //System.out.println("Calculated Sensor Range " + super.getSensorRange());
            int nodesinrange = 0;
           for(GeneratorNode node : nodes)
           {
               //("Distance " + node.getName() + "=" + calcDistance(node, sensor));
              if(calcDistance(node, sensor) < super.getSensorRange() && !node.isSensorNode() && !localCoveredNodes.contains(node))
              {
                  nodesinrange++;
                  localCoveredNodes.add(node);
              }
           }
            return nodesinrange;
        }
         
          public int pollNode(List<GeneratorNode> nodes, GeneratorNode sensor)
        {
            //System.out.println("Calculated Sensor Range " + super.getSensorRange());
            int nodesinrange = 0;
           for(GeneratorNode node : nodes)
           {
               //("Distance " + node.getName() + "=" + calcDistance(node, sensor));
              if(calcDistance(node, sensor) < super.getSensorRange() && !node.isSensorNode() && !coverednodes.contains(node))
              {
                  nodesinrange++;
              }
           }
            return nodesinrange;
        }
        
        public int pollPosition(List<GeneratorNode> nodes, DataLocation pollLocation)
        {
            //System.out.println("Calculated Sensor Range " + super.getSensorRange());
            int nodesinrange = 0;
           for(GeneratorNode node : nodes)
           {
               //("Distance " + node.getName() + "=" + calcDistance(node, sensor));
              if(calcDistance(node.getLocation().getX(), pollLocation.getX(), node.getLocation().getY(), pollLocation.getY()) < super.getSensorRange() && !node.isSensorNode() && !coverednodes.contains(node))
              {
                  nodesinrange++;
              }
           }
            return nodesinrange;
        }
        
        public double calcDistance (GeneratorNode nodeone, GeneratorNode nodetwo)
        {
           return  Math.sqrt((nodeone.getLocation().getX()-nodetwo.getLocation().getX())*(nodeone.getLocation().getX()-nodetwo.getLocation().getX()) + (nodeone.getLocation().getY()-nodetwo.getLocation().getY())*(nodeone.getLocation().getY()-nodetwo.getLocation().getY()));
        }
        
        public double calcDistance(int x1, int x2, int y1, int y2)
        {
            return Math.sqrt((x1 - x2) * (x1 -x2) + (y1 - y2)*(y1-y2));
        }
        
        public DataLocation calculateAverageCenter (List<GeneratorNode> nodes)
        {
            int x=0;
            int y=0;
            
            for(GeneratorNode node : nodes)
            {
                x+=node.getLocation().getX();
                y+=node.getLocation().getY();
            }
            x= x/nodes.size();
            y = y/nodes.size();
            
            return new DataLocation(x, y);
        }
        
        

    public void updateRanges(){
        if (perGroupRange) {
            List<GeneratorNode> groupNodes = new LinkedList<GeneratorNode>();
            for (NodeInGroup groupLeader : leaderGroup.keySet()) {
                double leaderRange = 0;
                for (NodeInGroup anyNodeInGroup : leaderGroup.get(groupLeader).getNodes()) {
                    if (!anyNodeInGroup.isLeader()) {
                        groupNodes.add(anyNodeInGroup.getNode());
                    } else {
                        leaderRange = anyNodeInGroup.getNode().getRange();
                    }
                }
                ((PowerAlgorithm) rangeSelect.getValue()).setRange(groupNodes);
                groupLeader.getNode().setRange(Math.max(groupLeader.getNode().getRange(), leaderRange));
                groupNodes.clear();
            }
        } else {
            super.updateRanges();
        }
    }

    @Override
    public Map<GeneratorNode, NodePainter> getNodeNodePainter() {
        NodePainter nodePainter1 = getNodePainter();
        Location myOffset = nodePainter1.getOffset();
        java.util.Map<GeneratorNode, NodePainter> outputMap = new HashMap<GeneratorNode, NodePainter>();
        List<GeneratorNode> nodesList = getModelNodes();
        Map<GeneratorNode, NodePainter> leadersNodePainterMap = getLeadersModel().getNodeNodePainter();
        for (GeneratorNode node : nodesList) {
            if (nodeNodeInGroup.get(node).isLeader()) {

                NodePainter leaderNodePainter = leadersNodePainterMap.get(node);
                leaderNodePainter.getOffset().translate(myOffset.getX() + leadersModelOffset.getX(),
                        myOffset.getY() + leadersModelOffset.getY());
                outputMap.put(node, leaderNodePainter);
            } else {
                outputMap.put(node, new GroupMemberNodePainter((GroupMemberNodePainter) nodePainter1));
            }

        }

        return outputMap;
    }

    @Override
    public void paintBackground(Graphics2D g) {
        edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.model.maps.Map map = getMap();
        g.translate(-map.getOrigin().getX(), -map.getOrigin().getY());
        g.translate(+leadersModelOffset.getX(), +leadersModelOffset.getY());
        getLeadersModel().paintBackground(g);
        g.translate(-leadersModelOffset.getX(), -leadersModelOffset.getY());
        getMap().paint(g);
        g.translate(+map.getOrigin().getX(), +map.getOrigin().getY());
    }

    /**
     * used to put member nodes around leader node using maxinitialdistance
     */
    public void initNodes() throws ModelInitializationException {
             usedSensors.clear();
             sensors.clear();
        getLeadersModel().initNodes();

        for (NodeInGroup groupLeader : leaderGroup.keySet()) {
            for (NodeInGroup nodeInGroup : leaderGroup.get(groupLeader).getNodes()) {
                if (!nodeInGroup.isLeader()) {
                    initNode(nodeInGroup.getNode());
                }
                if(nodeInGroup.node.isSensorNode())
                {
                    SensorNode s = new SensorNode(nodeInGroup.node);
                    //anyNodeInGroup.node.setSpeed(1);
                        s.setSpeed(1);
                    if(!sensors.contains(s))
                    {
                    sensors.add(s);
                    }
                }
            }
        }
        updateRanges();
    }

    @Override
    protected void getNextStep(double timeStep, GeneratorNode node) {
        //update member node location according to speed and destNode location and map reflection
        Location loc = node.getDoubleLocation();
        //creating the next location according to current speed and angle
        Location nextStepLoc = new Location(loc.getX() + node.getSpeed() * timeStep * Math.cos(node.getDirection()),
                loc.getY() + node.getSpeed() * timeStep * Math.sin(node.getDirection()));
        //checks if it is hit the border reflect it
        Location mirror = new Location(nextStepLoc.getX(), nextStepLoc.getY());
        //mirror will be the mirror point of the nextstep location
        Location hit = ((ReflectiveMap) getMap()).isHitBorder(loc, nextStepLoc, mirror);
        if (hit != null) {
            double timePassed = (mirror.getLength(hit) + hit.getLength(loc)) / node.getSpeed();
            //if it hit because in next timeStep new transition should be created now
            // new transition is not created and only mirror position used
            loc.pasteCoordination(mirror);
            node.setDirection(Location.calculateRadianAngle(hit, mirror));
            getNextStep(timeStep - timePassed, node);

        } else {
            loc.pasteCoordination(nextStepLoc);
        }
//                    DevelopmentLogger.logger.debug(anyNode.getName()+" : "+nextStepLoc +" : "+mirror +" : "+ hit);

        GeneratorNode leaderNode = nodeNodeInGroup.get(node).getGroup().getLeader().getNode();
        updateGroupNodeProperties(node, leaderNode);
    }
    
      protected void getNextSensorStep(double timeStep, GeneratorNode node, HashSet localCoveredNodes) {
          
          Location BestPosition = positionSensors(this.sensors.get(this.sensors.indexOf(node)), localCoveredNodes, modelNodes);
        //update member node location according to speed and destNode location and map reflection
        Location loc = node.getDoubleLocation();
        node.setDirection(Location.calculateRadianAngle(loc, BestPosition));
        
        //creating the next location according to current speed and angle
        Location nextStepLoc = new Location(loc.getX() + node.getSpeed() * timeStep * Math.cos(node.getDirection()),
                loc.getY() + node.getSpeed() * timeStep * Math.sin(node.getDirection()));
       
        //checks if it is hit the border reflect it
      /*  Location mirror = new Location(nextStepLoc.getX(), nextStepLoc.getY());
        //mirror will be the mirror point of the nextstep location
        Location hit = ((ReflectiveMap) getMap()).isHitBorder(loc, nextStepLoc, mirror);
        if (hit != null) {
            double timePassed = (mirror.getLength(hit) + hit.getLength(loc)) / node.getSpeed();
            //if it hit because in next timeStep new transition should be created now
            // new transition is not created and only mirror position used
            loc.pasteCoordination(mirror);
            node.setDirection(Location.calculateRadianAngle(hit, mirror));
            getNextStep(timeStep - timePassed, node);

        } else {
            loc.pasteCoordination(nextStepLoc);
        }*/
        loc.pasteCoordination(nextStepLoc);
//                    DevelopmentLogger.logger.debug(anyNode.getName()+" : "+nextStepLoc +" : "+mirror +" : "+ hit);

        GeneratorNode leaderNode = nodeNodeInGroup.get(node).getGroup().getLeader().getNode();
        updateGroupNodeProperties(node, leaderNode);
    }

    protected abstract void updateGroupNodeProperties(GeneratorNode node, GeneratorNode leaderNode);

    /**
     * @return number of nodes according to groups size
     */
    protected int sumGroupNodes() {
        int nodeNumber = 0;
        for (Integer num : groupMembersNum) {
            nodeNumber += num;
        }
        return nodeNumber;
    }

    public MultiMapHandleGroup getHandles() {
        MapHandleGroup leadersHandleGroup;
        MapHandleSupport leadersHandleSupport;
        Model leadersModel1 = getLeadersModel();
        leadersHandleSupport = leadersModel1.getMapHandleSupport();
        leadersHandleGroup = leadersHandleSupport.getHandles();
        MapHandleGroup membersHandleGroup = getMap().getHandles();
        //size and origin is set by membersMapHandleGroup which means that it will enclose leaders map
        MultiMapHandleGroup mmhg = new MultiMapHandleGroup(
                membersHandleGroup.getSize(), membersHandleGroup, this);
        mmhg.addMapHandleGroup(leadersHandleGroup, leadersHandleSupport,
                new MapHandle((int) leadersModelOffset.getX(), (int) leadersModelOffset.getY()));
        return mmhg;
    }

    public boolean validateHandles(MapHandleGroup mhp) {
        MultiMapHandleGroup mapHandleGroup = (MultiMapHandleGroup) mhp;
        edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.model.maps.Map membersMap = getMap();
        MapHandleSupport leadersModelSupport = mapHandleGroup.getSupporterFor(0);

        MapHandleGroup membersMhp = mapHandleGroup.getMasterHandleGroup();
        MapHandleGroup leadersMhp = mapHandleGroup.getMapHandleGroup(0);
        boolean valid = true;
        MapHandle offset = mapHandleGroup.getOffset(0);
        List<MapHandle> includingLocationsOffset = new LinkedList<MapHandle>();
        try {
            List<MapHandle> includingLocations = ((IncludableMap) leadersModelSupport).getIncludingLocations(leadersMhp);
            for (MapHandle includingLocation : includingLocations) {
                includingLocationsOffset.add(new MapHandle(includingLocation.getX() + offset.getX(),
                        includingLocation.getY() + offset.getY()));
            }
        } catch (ClassCastException e) {
            throw new SubModelException("Unable to include the leaders' model");
        }
        valid = valid && ((IncludingMap) membersMap).isIncluding(
                includingLocationsOffset, membersMhp);
        valid = valid && membersMap.validateHandles(membersMhp);
        valid = valid && leadersModelSupport.validateHandles(leadersMhp);
        return valid;
    }

    public void paintUsingHandles(Graphics2D g, MapHandleGroup mhp) {
        MultiMapHandleGroup mapHandleGroup = (MultiMapHandleGroup) mhp;
        getMap().paintUsingHandles(g, mapHandleGroup.getMasterHandleGroup());
        MapHandle offset = mapHandleGroup.getOffset(0);
        g.translate(+offset.getX(), +offset.getY());
        mapHandleGroup.getSupporterFor(0).paintUsingHandles(g, mapHandleGroup.getMapHandleGroup(0));
        g.translate(-offset.getX(), -offset.getY());
    }

    public void fillFromHandles(MapHandleGroup mhp) throws InvalidParameterInputException {
        MultiMapHandleGroup mapHandleGroup = (MultiMapHandleGroup) mhp;
        getMap().fillFromHandles(mapHandleGroup.getMasterHandleGroup());
        mapHandleGroup.getSupporterFor(0).fillFromHandles(mapHandleGroup.getMapHandleGroup(0));

        MapHandle offset = mapHandleGroup.getOffset(0);
        leadersModelOffset.pasteCoordination(offset.getX(), offset.getY());
    }

    public List<MapHandle> getIncludingLocations(MapHandleGroup mhp) {
        MultiMapHandleGroup mapHandleGroup = (MultiMapHandleGroup) mhp;
        edu.sharif.ce.dml.mobisim.mobilitygenerator.simulation.model.maps.Map membersMap = getMap();
        MapHandleSupport leadersModelSupport = mapHandleGroup.getSupporterFor(0);
        MapHandle offset = mapHandleGroup.getOffset(0);
        List<MapHandle> includingLocationsOffset = new LinkedList<MapHandle>();
        try {
            List<MapHandle> includingLocations = ((IncludableMap) leadersModelSupport).
                    getIncludingLocations(mapHandleGroup.getMapHandleGroup(0));
            for (MapHandle includingLocation : includingLocations) {
                includingLocationsOffset.add(new MapHandle(includingLocation.getX() + offset.getX(),
                        includingLocation.getY() + offset.getY()));
            }
        } catch (ClassCastException e) {
            throw new SubModelException("Unable to include the leaders' model");
        }
        includingLocationsOffset.addAll(((IncludableMap) membersMap).
                getIncludingLocations(mapHandleGroup.getMasterHandleGroup()));
        return includingLocationsOffset;

    }

    public List print(GeneratorNode node, Location offset) {
        AbstractGroupModel.NodeInGroup nodeInGroup = nodeNodeInGroup.get(node);
        List output = new LinkedList();
        if (nodeInGroup.isLeader()) {
            output.addAll(getLeadersModel().print(node, nodeInGroup.group.getOffset()));
        } else {
            output.addAll(node.print(offset));
        }
        if (extraTraces) {
            output.add(nodeInGroup.group.getGroupId());
            output.add(nodeInGroup.isLeader());
        }
        
      
        return output;
    }

    public List<String> getLabels() {
        if (extraTraces) {
            List<String> output = new LinkedList<String>(getLeadersModel().getLabels());
            output.addAll(Arrays.asList(traceLabels));
            return output;
        } else {
            return getLeadersModel().getLabels();
        }
    }

    /**
     * representing a node in a group it contains leader node and member node.
     * it can be refined by making a hierarchy of node classes
     */
    protected class NodeInGroup implements Comparable<NodeInGroup> {
        public static final String GROUP_LEADER = "leader";
        public static final String GROUP_MEMBER = "member";
        protected GeneratorNode node;
        protected String type;
        protected Group group;

        /**
         * paints node according to leader or member node
         */

        public NodeInGroup(GeneratorNode node, String type, Group group) {
            this.node = node;
            this.type = type;
            this.group = group;
        }

        public int hashCode() {
            return this.node.hashCode();
        }

        /**
         * two group node are equal if their node are equal
         *
         * @param obj
         * @return
         */
        public boolean equals(Object obj) {
            return this.node.equals(((NodeInGroup) obj).node);
        }

        public GeneratorNode getNode() {
            return node;
        }

        public boolean isLeader() {
            return type.equals(NodeInGroup.GROUP_LEADER);
        }

        public Group getGroup() {
            return group;
        }

        public void setGroup(Group group) {
            this.group = group;
        }

        public int compareTo(NodeInGroup nodeInGroup) {
            if (this.getGroup().equals(nodeInGroup)) {
                if (this.isLeader()) {
                    return 1;
                } else if (nodeInGroup.isLeader()) {
                    return -1;
                } else {
                    return 0;
                }
            }
            return this.getGroup().compareTo(nodeInGroup.getGroup());
        }

        public Location getMovedLocation() {
            Location loc = new Location(node.getDoubleLocation());
            Location offset = group.getOffset();
            loc.translate(offset.getX(), offset.getY());
            return loc;
        }
        

    }
    
    /**
     * a group of {@link GeneratorNode} objects that form a cluster
     */
    protected class Cluster implements Comparable {
        protected int clusterSize;
        protected List<GeneratorNode> clusteredNodes;
        
        public Cluster() {
            this.clusterSize = 0;
            this.clusteredNodes = new ArrayList<GeneratorNode>();
        }
        
        public Cluster(List<GeneratorNode> nodes) {
            this.clusteredNodes = nodes;
            this.clusterSize = nodes.size();
        }
        
        public void add(GeneratorNode node) {
            this.clusteredNodes.add(node);
            this.clusterSize++;
        }
        
        public void addAll(List<GeneratorNode> nodes) {
            this.clusteredNodes.addAll(nodes);
            this.clusterSize += nodes.size();
        }
        
        public void remove(GeneratorNode node) {
            this.clusteredNodes.remove(node);
            this.clusterSize--;
        }
        
        public void remove(int index) {
            this.clusteredNodes.remove(index);
            this.clusterSize--;
        }
        
        public List<GeneratorNode> getNodes() {
            return this.clusteredNodes;
        }
        
        public GeneratorNode getNode(int index) {
            return this.clusteredNodes.get(index);
        }
        
        public int getClusterSize() {
            return this.clusterSize;
        }
        
        @Override
        public int compareTo(Object cluster) {
            Cluster otherCluster = (Cluster)cluster;
            if (this.clusterSize == otherCluster.getClusterSize()) {
                return 0;
            }
            else if (this.clusterSize > otherCluster.getClusterSize()) {
                return 1;
            }
            else {
                return -1;
            }
        }
    }

    /**
     * a group of {@link NodeInGroup} objects that have a leader.
     */
    protected class Group implements Comparable {
        protected final Location offset;
        protected int groupId;
        List<NodeInGroup> nodes = new LinkedList<NodeInGroup>();
        NodeInGroup leader;
        private int size = -1;
        private Color color = null;

        public Group(NodeInGroup leader, int groupId) {
            this.leader = leader;
            this.groupId = groupId;
            Location leadersOrigin = getLeadersModel().getMap().getOrigin();
            Location myOrigin = getMap().getOrigin();
            offset = new Location(-leadersOrigin.getX() + myOrigin.getX() + leadersModelOffset.getX(),
                    -leadersOrigin.getY() + myOrigin.getY() + leadersModelOffset.getY());
        }

        public List<NodeInGroup> getNodes() {
            
            return nodes;
        }

        public void setNodes(List<NodeInGroup> nodes) {
            this.nodes = nodes;
        }

        public NodeInGroup getLeader() {
            return leader;
        }

        public Location getLeaderLoc() {
            return leader.getMovedLocation();
        }

        public Location getOffset() {
            return offset;
        }

        public boolean equals(Object obj) {
            return groupId == ((Group) obj).groupId;
        }

        /**
         * note that it uses Integer.parseInt and it may generate {@link NumberFormatException}
         *
         * @param g
         * @return the numerical difference in groupID of groups * 2
         */
        public int compareTo(Object g) {
            return ((groupId) - ((Group) g).groupId) * 2;
        }

        public int getGroupId() {
            return groupId;
        }

        /**
         * @param node a non leader node
         * @return
         */
        public int getSize(GeneratorNode node) {
            if (size < 0) {
                size = nodePainter.getSize(node);
            }
            if(node.isSensorNode())
            {
                size = nodePainter.getSize(node) * 5;
            }
            return size;
        }

        public Color getColor(GeneratorNode node) {
            if (color == null) {
                color = nodePainter.getColor(node);
                if(node.isSensorNode())
                {
                    color = Color.RED;
                }
            }
            return color;
        }

        
    }

    protected class GroupMemberNodePainter extends NodePainter {
        NodePainter leaderPainter;

        public GroupMemberNodePainter(NodePainter leaderPainter) {
            super();
            this.leaderPainter = leaderPainter;
        }

        public GroupMemberNodePainter(GroupMemberNodePainter nodePainter) {
            super(nodePainter);
            this.leaderPainter = nodePainter.leaderPainter;
        }

        public int getSize(GeneratorNode node) {
            return (int) (2.0 * leaderPainter.getSize(nodeNodeInGroup.get(node).getGroup().getLeader().getNode()) / 3);
        }

        public Color getColor(GeneratorNode node) {
            return leaderPainter.getColor(nodeNodeInGroup.get(node).getGroup().getLeader().getNode());
        }
        

    }
    public class CandidateLocation
    {
        public int coverage;
        public SensorNode basenode;
        public int position;
        public DataLocation candidateLocation;
        
        public CandidateLocation(int coverage, SensorNode basenode, int position, DataLocation candidateLocation)
        {
            this.coverage = coverage;
            this.basenode = basenode;
            this.position = position;
            this.candidateLocation = candidateLocation;
        }
    }
}
