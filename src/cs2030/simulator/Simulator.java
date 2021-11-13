package cs2030.simulator;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Queue;

public class Simulator {

    private final int numOfServers;
    private final ArrayList<Double> arrivalTimes;
    private final ArrayList<Double> serviceTimes;
    private final int maxLength;
    private final ArrayList<Double> restTimes;
    private final int selfCheckout;
    private static final int ZERO = 0;
    private static final int ONE = 1;

    public Simulator(int num,
                     ArrayList<Double> arrivalTimes,
                     ArrayList<Double> serviceTimes,
                     int maxLength,
                     ArrayList<Double> restTimes,
                     int selfCheckout) {
        this.numOfServers = num;
        this.arrivalTimes = arrivalTimes;
        this.serviceTimes = serviceTimes;
        this.maxLength = maxLength;
        this.restTimes = restTimes;
        this.selfCheckout = selfCheckout;
    }


    // execute the simulation
    public void simulate() {
        // Simulation Statistics

        double totalWait = ZERO;
        int numServed = ZERO;
        int numLeft = ZERO;

        PriorityQueue<Event> events = new PriorityQueue<>();

        int totalServers = this.numOfServers + this.selfCheckout;
        Store store = new Store(totalServers, this.numOfServers, this.maxLength);

        for (int i = ZERO; i < this.arrivalTimes.size(); i++) {

            Customer c1 = new Customer(i + ONE,
                    this.arrivalTimes.get(i),
                    this.serviceTimes.get(i));

            // Instantiate the arrival first, execute them later
            events.add(new ArrivalEvent(c1));
        }

        int curRest = ZERO;

        while (!events.isEmpty()) {

            // Get current event and print
            // Ok if arrival event, as nextEvent(store) solves serve problem

            Event cur = events.poll();
            if (!(cur instanceof BreakEvent)) {
                System.out.println(cur);
            }
            // Execute the event and update the store
            Occur updateBoth = cur.nextEvent(store);


            // Update the store for execution in next line
            store = updateBoth.getStore();
            Event e = updateBoth.getEvent();

            Server eventServer = cur.getServer();
            int curServerID = cur.getServer().getId();
            int index = curServerID - ONE;

            if ((cur instanceof DoneEvent) &&
                    (eventServer.isHuman())) {
                // Approach:
                // After each done event, execute a new Serve event
                // if there are still customers in the queue
                // Extend this to include Wait events as well

                // Server takes a break
                double breakTime = this.restTimes.get(curRest);
                curRest++;
                // Update the next serve time of the server
                double nextTime = breakTime + cur.getTime();

                store = store.addSlack(curServerID, nextTime);
                Server curS = store.getServer(index);

                /*System.out.println("canWait? : " + curS.isWorking());
                System.out.println("Break Time " + breakTime);
                System.out.println("Server nexTime "+curS.getWhenServe());
                System.out.println(String.format("serve time: %.3f", nextTime));*/
                Queue<Customer> queue = store.getQueue(index);
                //System.out.println("Queue Length: " + queue.size());
                if (!queue.isEmpty()) {
                    Customer waiting = queue.poll();
                    store = store.updateServeTime(index, nextTime);

                    // Update the serve time
                    Event serverBreak = new BreakEvent(
                            waiting,
                            curS,
                            nextTime);
                    events.add(serverBreak);
                } else {
                    store = store.unSlack(index);
                }

            } else if (cur instanceof DoneEvent) {
                // Skip break event because SCO has no rest
                // Get the server
                Server checkout = store.getServer(index);
                Queue<Customer> queue = store.getSCOQueue();

                //System.out.println("Server nexTime "+ checkout.getWhenServe());
                //System.out.println("can Serve? : " + checkout.getAvail());
                if (!queue.isEmpty()) {
                    Customer waitingCustomer = queue.poll();
                    double serveEventTime = cur.getTime();
                    Event serveEvent = new ServeEvent(
                            waitingCustomer,
                            checkout,
                            serveEventTime);
                    events.add(serveEvent);
                }
            }


            // Handling break Events
            if (cur instanceof BreakEvent) {
                store = store.unSlack(index);
                Event serveEvent = new ServeEvent(
                        cur.getCust(),
                        cur.getServer(),
                        cur.getTime()
                );
                events.add(serveEvent);
            }


            if (cur instanceof ServeEvent) {
                totalWait += cur.getWait();
                numServed++;
            }  else if (cur instanceof LeaveEvent) {
                numLeft++;
            }


            // Adding the executed next event
            if (!(e instanceof Final)) {
                events.add(e);
            }
        }

        double averageWait = totalWait / (double) numServed;

        System.out.println(String.format("[%.3f %d %d]", averageWait,
                numServed,
                numLeft));
    }


}