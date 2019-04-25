import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class FordFulkerson {
	
	SimpleGraph graph;
	int maxflow;
	Vertex s;
	Vertex t;
	
	/**
	 * Constructor for the FordFulkerson class, takes in the graph and locates the source and sink vertices
	 * @param g the graph on which the algorithm will be run
	 */
	public FordFulkerson(SimpleGraph g)
	{
		graph = g;
		s = findS(g);
		t = findT(g);
	}
	/**
	 * A helper method that increases the flow along each edge comprising a given path. Each time it augments a path it will also update the value
	 * of the maxflow by that amount.
	 * @param path a linked-list of edges that make up the path
	 */
	private void augment(LinkedList<Edge> path)
	{
		//finds the minimum remaining capacity among the edges of the path
		double remainingCapacity = (double)path.get(0).getData();
		for(int i = 1; i < path.size(); i++)
		{
			remainingCapacity = Math.min(remainingCapacity, (double)path.get(i).getData());
		}
		
		//for each edge in the path increases the flow by the bottleneck capacity
		for(int i = 0; i < path.size(); i++)
		{
			increaseFlow(path.get(i), remainingCapacity);
		}
		
		//increases the maxflow value by that amount
		maxflow += remainingCapacity;
	}
	
	
	/**
	 * A helper method that increases the flow along an edge (by decreasing its capacity) while also increasing the capacity of a residual edge
	 * @param e the edge to increase the flow along
	 * @param i the amount of flow to add
	 */
	private void increaseFlow(Edge e, double i)
	{
		//Decrease the remaining capacity of edge e by the given value
		e.setData((double)e.getData()-i);
		
		//locate a "residual edge" in the graph which corresponds with e, if none exist this method will add one and return it.
		Edge r = getMatchingEdge(e);
		
		//Increase the remaining capacity of that residual edge by the given value
		r.setData((double)r.getData()+i);
	}
	
	/**
	 * A helper method which finds an augmenting path in the graph with bottleneck capacity of at least delta
	 * @param delta the minimum allowable capacity of edges to be included in the path
	 * @return a linked list of edges which comprise the desired s-t path
	 */
	private LinkedList<Edge> augmentingPath(int delta)
	{
		//Initialize a list of previously visited vertices and the edge they were reached by, record s as visited
		HashMap<Vertex, Edge> visited = new HashMap<Vertex, Edge>();
		visited.put(s,null);
		
		//Initialize a stack for DFS, can be changed to a queue
		LinkedList<Edge> check = new LinkedList<Edge>();
		
		//Initialize some variables ahead of time
		Vertex v = s;
		Edge e, f;
		Iterator j;
		
		//Add all the edges out of s to the stack
		for(j = s.incidentEdgeList.iterator(); j.hasNext();)
		{
			e = (Edge)j.next();
			//if the edge has capacity greater than delta and is outgoing from s add it to the stack
			if((double)e.getData()>=delta && s.equals(e.getFirstEndpoint())) check.add(e);
		}
		
		//While there are still edges left in the stack and we haven't reached t
		while(!v.equals(t) && !check.isEmpty())
		{
			//pop an edge off the stack and get its second endpoint
			e = check.pop();
			v = e.getSecondEndpoint();
			
			//If that vertex has not yet been visited by the search
			if(!visited.containsKey(v))
			{
				//record the edge by which v was reached, in the process marking it as visited.
				visited.put(v, e);
				
				//Look through its incedent edges
				for(j = v.incidentEdgeList.iterator(); j.hasNext();)
				{
					f = (Edge)j.next();
					//if the edge has capacity greater than or equal to delta and is outgoing from v, and is unvisited add it to the stack
					if((double)f.getData()>=delta && v.equals(f.getFirstEndpoint()) && !visited.containsKey(f.getSecondEndpoint())) check.add(f);
				}
			}
		}
		
		//If the stack is empty and has not reached t then no path exists, return null
		if(!v.equals(t))
		{
			return null;
		}
		
		//create a list of edges to return the path that was found
		LinkedList<Edge> out = new LinkedList<Edge>();
		
		//we know v is currently equal to t, when it is equal to s again we know we've recorded the whole path.
		while(!v.equals(s))
		{
			//take the edge that v was reached by
			e = visited.get(v);
			//add e to the output set
			out.add(e);
			//set v to the vertex that e came from
			v = e.getFirstEndpoint();
		}
		
		return out;
	}
	
	/**
	 * A helper method which finds a "residual edge" for a given input edge, if one does not already exist in the graph this method will create one
	 * @param e an edge in the graph
	 * @return an edge in the graph with the same endpoints as e but pointing the other direction
	 */
	private Edge getMatchingEdge(Edge e)
	{
		//initialize some variables ahead of time
		Edge out;
		Iterator j;
		Vertex v = e.getSecondEndpoint();
		Vertex w = e.getFirstEndpoint();
		
		//iterate through the incident edges of e's second endpoint
		for(j = v.incidentEdgeList.iterator(); j.hasNext();)
		{
			out = (Edge)j.next();
			//if one is found which ends at e's first endpoing return it
			if(out.getSecondEndpoint() == w) return out;			
		}
		//otherwise insert a new edge with zero capacity
		return graph.insertEdge(v, w, 0.0, null);
	}
	
	
	/**
	 * Performs the scaling ford fulkerson algorithm and returns the value of the maxflow
	 * @return the maxflow of the graph
	 */
	public double scalingMaxFlow()
	{
		//initialize some variables
		double maxOut = 0;
		Edge e;
		Iterator j;
		
		//compute the largest capacity out of s
		for(j = graph.incidentEdges(s); j.hasNext();)
		{
			e = (Edge) j.next();
			maxOut = Math.max(maxOut, (double)e.getData());
		}
		
		
		//Set the value delta equal to the largest power of 2 smaller than at least one capacity out of s
		int delta = 2;
		while(delta < maxOut) delta*=2;
		delta /= 2;
		
		//as long as delta is greater than or equal to 1
		while(delta >= 1)
		{
			//find an augmenting path in g
			LinkedList<Edge> path = augmentingPath(delta);

			//as long as the algorithm continues to find valid paths
			while (path!=null)
			{
				//augment flow along that path and then locate another path
				augment(path);
				path = augmentingPath(delta);
			}
			
			//when no more paths are found, reduce delta and continue the search
			delta /= 2;
		}
		//return the maxflow value which has been maintained by the augment helper method.
		return maxflow;
	}
	
	/**
	 * Performs the standard Ford Fulkerson algorithm and returns the maxflow
	 * @return the value of the maxflow
	 */
	public double maxFlow()
	{
		//Initialize some variables for later use
		Edge e;
		Iterator j;
		
		//Find an augmenting path with a delta value equal to one, which will consider all edges with capacity remaining
		LinkedList<Edge> path = augmentingPath(1);
		
		//while the algorithm continues to find paths
		while (path!=null)
		{
			//augment along those paths and continue searching for paths
			augment(path);
			path = augmentingPath(1);
		}
		
		//return the value of the maxflow which has been maintained by the augment helper method
		return maxflow;
	}
	
	/**
	 * Locates the source node in the graph, it makes the assumption that said node will be named "s" and that no other node will share that name.
	 * @param g The graph
	 * @return the Vertex that is to be designated the source for the flow
	 */
	private static Vertex findS(SimpleGraph g)
	{
		//search all the vertices but start at the beginning since in the graphs we were given the source was the first one
		for(int i = 0; i < g.vertexList.size(); i++)
		{
			Vertex v = (Vertex)g.vertexList.get(i);
			if(v.getName().equals("s")) return v;
		}
		return null;
	}
	
	/**
	 * Locates the source node in the graph, it makes the assumption that said node will be named "t" and that no other node will share that name.
	 * @param g The graph
	 * @return the Vertex that is to be designated the source for the flow
	 */
	public static Vertex findT(SimpleGraph g)
	{
		//search all the vertices, but start at the end since the sink was usually at the end in the graphs we were given
		for(int i = g.vertexList.size()-1; i >= 0; i--)
		{
			Vertex v = (Vertex)g.vertexList.get(i);
			if(v.getName().equals("t")) return v;
		}
		return null;
	}
}
