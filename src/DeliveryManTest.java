

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import aStar.AStarTraversalAuto;
import aStar.AStarTraversalHuman;
import vehicles.agents.Truck;
import restaurant.JPC.RestaurantJPC;
import simCity.PersonAgent;
import simCity.YellowPages;
import simCity.business.Market;
import simCity.business.Restaurant;
import simCity.config.SimCityPanel;
import simCity.gui.AnimationPanelSimCity;
import simCity.gui.PersonGui;
import simCity.gui.TruckGui;
import junit.framework.TestCase;
import market.DeliveryManRole;
import market.DeliveryOrder;
import market.gui.MarketAnimationPanel;
import market.gui.MarketDeliveryManGui;
import market.gui.MarketDeliveryManSimCityGui;
import market.gui.MarketStatusPanel;
import market.test.mock.MockCook;
import market.test.mock.MockGetter;
import market.test.mock.MockMarketCashier;
import market.test.mock.MockRestaurant;
import market.test.mock.MockRestaurantCashier;
import market.DeliveryOrder.DeliveryOrderState;

public class DeliveryManTest extends TestCase {
	// Non-Mock
	DeliveryManRole deliveryMan;
	DeliveryManRole deliveryMan2;
	MarketDeliveryManGui dmGUI;
	MarketDeliveryManGui dmGUI2;
	MarketDeliveryManSimCityGui simCityGUI;
	MarketDeliveryManSimCityGui simCityGUI2;
	MarketAnimationPanel marketAnim;
	Market market;
	PersonAgent person;
	PersonAgent person2;
	YellowPages yp;
	AStarTraversalHuman aStarHuman;
	AStarTraversalAuto aStarAuto;
	AnimationPanelSimCity animationPanel;
	// Mock Interactors
	MockMarketCashier marketCashier;
	MockRestaurantCashier restaurantCashier;
	MockRestaurant restaurant;
	MockRestaurant restaurant2;
	MockCook cook;
	MockGetter getter;
	Truck truck1;
	Truck truck2;

	public void setUp() throws Exception{
		super.setUp();
		person = new PersonAgent();
		person2 = new PersonAgent();
		market = new Market("Market1", new Point(555, 220), null, marketAnim, "Generic");
		MarketStatusPanel msp = new MarketStatusPanel(market);
		market.setConfigPanel(msp);
		marketCashier = new MockMarketCashier("Market Cashier");
		cook = new MockCook("Cook");
		getter = new MockGetter("Getter");
		market.addGetter(getter);
		restaurantCashier = new MockRestaurantCashier("Restaurant Cashier");
		restaurant = new MockRestaurant("Restaurant", new Point(100, 100), new Point(100, 100));
		restaurant2 = new MockRestaurant("Restaurant2", new Point(200, 200), new Point(200, 200));
		restaurant.addCashier(restaurantCashier);
		restaurant.addCook(cook);
		restaurant2.addCashier(restaurantCashier);
		restaurant2.addCook(cook);
//		truck1 = new Truck(market);
//		truckGUI = new TruckGui(truck1);
//		truck1.setGui(truckGUI);
//		truck2 = new Truck(market);
//		truck2GUI = new TruckGui(truck2);
//		truck2.setGui(truck2GUI);

		// Delivery Men
		// First
		deliveryMan = new DeliveryManRole(person, market);
		deliveryMan.setCashier(marketCashier);
//		deliveryMan.setTruck(truck);
		dmGUI = new MarketDeliveryManGui(deliveryMan);
		simCityGUI = new MarketDeliveryManSimCityGui(deliveryMan, market);
		deliveryMan.setGui(dmGUI);
		deliveryMan.setCityGui(simCityGUI);

		// Second
		deliveryMan2 = new DeliveryManRole(person2, market);
		deliveryMan2.setCashier(marketCashier);
		deliveryMan2.setTruck(truck2);
		dmGUI2 = new MarketDeliveryManGui(deliveryMan2);
		simCityGUI2 = new MarketDeliveryManSimCityGui(deliveryMan2, market);
		deliveryMan2.setGui(dmGUI2);
		deliveryMan2.setCityGui(simCityGUI2);
	}

	// Normative Tests
	// Test One - One Delivery Man fulfills Delivery Order from Cashier, collecting one item
	public void testOne_DeliveryMan_Normative(){
		// Preconditions
		assertEquals("DeliveryMan should have $0.00 in Delivery Payment. It doesn't.", deliveryMan.getDeliveryPayment(), 0.00);
		assertEquals("DeliveryMan should have an empty event log before HereAreItemsForDelivery is called. "
				+ "Instead, the Cashier's event log reads: " + deliveryMan.log.toString(), 0, deliveryMan.log.size());

		// Step 1 - DeliveryMan gets message from Getter saying delivery is needed
		List<String> food = new ArrayList<String>();
		food.add("Chicken");
		List<Integer> amount = new ArrayList<Integer>();
		amount.add(2);
		DeliveryOrder order = new DeliveryOrder(food, amount, restaurant, marketCashier, 10.00);
		order.state = DeliveryOrderState.Retrieved;
		deliveryMan.HereIsOrderToBeDelivered(order);
		assertTrue("Delivery Man should have a currentDelivery in it. It doesn't.", deliveryMan.getCurrentDelivery() != null);
		assertTrue("Current delivery items size should equal 1. It doesn't.", deliveryMan.getCurrentDelivery().items.size() == 1);
		assertTrue("Current delivery items amounts size should equal 1. It doesn't.", deliveryMan.getCurrentDelivery().amounts.size() == 1);
		
		// Step 2 - Delivery Man calls scheduler and goes to restaurant
		deliveryMan.atExit.release();
		deliveryMan.atDestination.release();
		deliveryMan.atDestination.release();
		deliveryMan.atDestination.release();
		
		assertTrue("Delivery Man's scheduler should return true (next action is to go to restaurant), but didn't.", 
				deliveryMan.pickAndExecuteAnAction());
		assertTrue("Cashier should have logged \"Received DeliveryItemsAboutToBeDelivered\". Instead his log reads: " +
				marketCashier.log.getLastLoggedEvent().toString(), marketCashier.log.containsString("Received DeliveryItemsAboutToBeDelivered"));
		assertTrue("Delivery Man's GUI X Destination should be 0, but isn't.", dmGUI.xDestination == 0);
		assertTrue("Delivery Man's GUI Y Destination should be 0, but isn't.", dmGUI.yDestination == 0);
		assertFalse("Delivery Man's isPresent should be false, but isn't.", dmGUI.isPresent());
		assertFalse("Delivery Man's SimCityGUI isPresent should be false.", simCityGUI.isPresent());
		assertTrue("Delivery Man's City GUI X Destination should be 100, but isn't.", simCityGUI.xDestination == 100);
		assertTrue("Delivery Man's City GUI Y Destination should be 100, but isn't.", simCityGUI.yDestination == 100);

		// Step 3 - Delivery Man gives cook of restaurant the order
		assertTrue("Cook should have logged \"Received HereIsDelivery\". Instead his log reads: " +
				cook.log.getLastLoggedEvent().toString(), cook.log.containsString("Received HereIsDelivery"));
		assertTrue("Restaurant Cashier should have logged \"Received NeedDeliveryPayment\". Instead his log reads: " +
				restaurantCashier.log.getLastLoggedEvent().toString(), restaurantCashier.log.containsString("Received NeedDeliveryPayment"));

		// Step 4 - Restaurant Cashier gives payment to Delivery Man
		deliveryMan.HereIsDeliveryPayment(6.00);
		assertTrue("Delivery Payment should be 6.00", deliveryMan.getDeliveryPayment() == 6.00);

		// Step 5 - Delivery Man calls scheduler and leaves restaurant
		deliveryMan.atDestination.release();
		deliveryMan.atDestination.release();
		deliveryMan.atDestination.release();
		assertTrue("Delivery Man's scheduler should return true (next action is to leave restaurant), but didn't.", 
				deliveryMan.pickAndExecuteAnAction());
		assertTrue("Current Delivery should be null", deliveryMan.getCurrentDelivery() == null);
		assertTrue("Delivery Man GUI should be present", dmGUI.isPresent());
		assertFalse("Delivery Man SimCity GUI should not be present", simCityGUI.isPresent());

		// Step 6 - Delivery Man calls scheduler and gives payment to Market Cashier
		deliveryMan.atCashier.release();
		assertTrue("Delivery Man's scheduler should return true (next action is to give payment to Cashier), but didn't.", 
				deliveryMan.pickAndExecuteAnAction());
		assertTrue("Delivery Man's GUI X Destination should be 450, but isn't.", dmGUI.xDestination == 450);
		assertTrue("Delivery Man's GUI Y Destination should be 200, but isn't.", dmGUI.yDestination == 200);
		assertTrue("Market Cashier should have logged \"Received HereIsPayment for $6.0\". Instead his log reads: " +
				marketCashier.log.getLastLoggedEvent().toString(), marketCashier.log.containsString("Received HereIsPayment for $6.0"));
		assertTrue("Getter should have logged \"Received IAmAvailable\". Instead his log reads: " +
				getter.log.getLastLoggedEvent().toString(), getter.log.containsString("Received IAmAvailable"));
		assertTrue("Delivery Payment should be 0", deliveryMan.getDeliveryPayment() == 0);

		// Step 7 - No further action
		assertFalse("Delivery Man's scheduler should return false (no actions remain), but didn't.", 
				deliveryMan.pickAndExecuteAnAction());
	}

	// Test Two - One Delivery Man fulfills Delivery Order from Cashier, collecting two items
	public void testTwo_DeliveryMan_Normative(){
		// Preconditions
		assertEquals("DeliveryMan should have $0.00 in Delivery Payment. It doesn't.", deliveryMan.getDeliveryPayment(), 0.00);
		assertEquals("DeliveryMan should have an empty event log before HereAreItemsForDelivery is called. "
				+ "Instead, the Cashier's event log reads: " + deliveryMan.log.toString(), 0, deliveryMan.log.size());

		// Step 1 - DeliveryMan gets message from Cashier saying delivery is needed
		List<String> food = new ArrayList<String>();
		food.add("Chicken");
		food.add("Onion");
		List<Integer> amount = new ArrayList<Integer>();
		amount.add(2);
		amount.add(4);
		DeliveryOrder order = new DeliveryOrder(food, amount, restaurant, marketCashier, 10.00);
		order.state = DeliveryOrderState.Retrieved;
		deliveryMan.HereIsOrderToBeDelivered(order);
		assertTrue("Delivery Man should have a currentDelivery in it. It doesn't.", deliveryMan.getCurrentDelivery() != null);
		assertTrue("Current delivery requested items size should equal 2. It doesn't.", deliveryMan.getCurrentDelivery().requestedItems.size() == 2);
		assertTrue("Current delivery requested items amounts size should equal 2. It doesn't.", deliveryMan.getCurrentDelivery().requestedItemsAmounts.size() == 2);

		// Step 2 - Delivery Man calls scheduler and goes to restaurant
		deliveryMan.atExit.release();
		deliveryMan.atDestination.release();
		deliveryMan.atDestination.release();
		deliveryMan.atDestination.release();
		assertTrue("Delivery Man's scheduler should return true (next action is to go to restaurant), but didn't.", 
				deliveryMan.pickAndExecuteAnAction());
		assertTrue("Cashier should have logged \"Received DeliveryItemsAboutToBeDelivered\". Instead his log reads: " +
				marketCashier.log.getLastLoggedEvent().toString(), marketCashier.log.containsString("Received DeliveryItemsAboutToBeDelivered"));
		assertTrue("Delivery Man's GUI X Destination should be 0, but isn't.", dmGUI.xDestination == 0);
		assertTrue("Delivery Man's GUI Y Destination should be 0, but isn't.", dmGUI.yDestination == 0);
		assertFalse("Delivery Man's isPresent should be false, but isn't.", dmGUI.isPresent());
		assertFalse("Delivery Man's SimCityGUI isPresent should be false.", simCityGUI.isPresent());
		assertTrue("Delivery Man's City GUI X Destination should be 100, but isn't.", simCityGUI.xDestination == 100);
		assertTrue("Delivery Man's City GUI Y Destination should be 100, but isn't.", simCityGUI.yDestination == 100);

		// Step 3 - Delivery Man gives cook of restaurant the order
		assertTrue("Cook should have logged \"Received HereIsDelivery\". Instead his log reads: " +
				cook.log.getLastLoggedEvent().toString(), cook.log.containsString("Received HereIsDelivery"));
		assertTrue("Restaurant Cashier should have logged \"Received NeedDeliveryPayment\". Instead his log reads: " +
				restaurantCashier.log.getLastLoggedEvent().toString(), restaurantCashier.log.containsString("Received NeedDeliveryPayment"));

		// Step 4 - Restaurant Cashier gives payment to Delivery Man
		deliveryMan.HereIsDeliveryPayment(8.00);
		assertTrue("Delivery Payment should be 8.00", deliveryMan.getDeliveryPayment() == 8.00);

		// Step 5 - Delivery Man calls scheduler and leaves restaurant
		deliveryMan.atDestination.release();
		deliveryMan.atDestination.release();
		deliveryMan.atDestination.release();
		assertTrue("Delivery Man's scheduler should return true (next action is to leave restaurant), but didn't.", 
				deliveryMan.pickAndExecuteAnAction());
		assertTrue("Current Delivery should be null", deliveryMan.getCurrentDelivery() == null);
		assertTrue("Delivery Man GUI should be present", dmGUI.isPresent());
		assertFalse("Delivery Man SimCity GUI should not be present", simCityGUI.isPresent());

		// Step 6 - Delivery Man calls scheduler and gives payment to Market Cashier
		deliveryMan.atCashier.release();
		assertTrue("Delivery Man's scheduler should return true (next action is to give payment to Cashier), but didn't.", 
				deliveryMan.pickAndExecuteAnAction());
		assertTrue("Delivery Man's GUI X Destination should be 450, but isn't.", dmGUI.xDestination == 450);
		assertTrue("Delivery Man's GUI Y Destination should be 200, but isn't.", dmGUI.yDestination == 200);
		assertTrue("Market Cashier should have logged \"Received HereIsPayment for $8.0\". Instead his log reads: " +
				marketCashier.log.getLastLoggedEvent().toString(), marketCashier.log.containsString("Received HereIsPayment for $8.0"));
		assertTrue("Getter should have logged \"Received IAmAvailable\". Instead his log reads: " +
				getter.log.getLastLoggedEvent().toString(), getter.log.containsString("Received IAmAvailable"));
		assertTrue("Delivery Payment should be 0", deliveryMan.getDeliveryPayment() == 0);

		// Step 7 - No further action
		assertFalse("Delivery Man's scheduler should return false (no actions remain), but didn't.", 
				deliveryMan.pickAndExecuteAnAction());
	}

	// Test Three - Two Delivery Men fulfills Delivery Order from Cashier, collecting one item each, and go to different restaurants
	public void testThree_DeliveryMan_Normative(){
		// Preconditions
		assertEquals("DeliveryMan should have $0.00 in Delivery Payment. It doesn't.", deliveryMan.getDeliveryPayment(), 0.00);
		assertEquals("DeliveryMan should have an empty event log before HereAreItemsForDelivery is called. "
				+ "Instead, the Cashier's event log reads: " + deliveryMan.log.toString(), 0, deliveryMan.log.size());
		assertEquals("DeliveryMan2 should have $0.00 in Delivery Payment. It doesn't.", deliveryMan2.getDeliveryPayment(), 0.00);
		assertEquals("DeliveryMan2 should have an empty event log before HereAreItemsForDelivery is called. "
				+ "Instead, the Cashier's event log reads: " + deliveryMan2.log.toString(), 0, deliveryMan2.log.size());

		// Step 1 - DeliveryMan gets message from Cashier saying delivery is needed
		List<String> food = new ArrayList<String>();
		List<Integer> amount = new ArrayList<Integer>();

		food.add("Chicken");
		amount.add(2);
		DeliveryOrder order = new DeliveryOrder(food, amount, restaurant, marketCashier, 10.00);
		order.state = DeliveryOrderState.Retrieved;
		deliveryMan.HereIsOrderToBeDelivered(order);
		
		List<String> food2 = new ArrayList<String>();
		List<Integer> amount2 = new ArrayList<Integer>();
		food2.add("Onion");
		amount2.add(4);
		DeliveryOrder order2 = new DeliveryOrder(food2, amount2, restaurant2, marketCashier, 10.00);
		order2.state = DeliveryOrderState.Retrieved;
		deliveryMan2.HereIsOrderToBeDelivered(order2);
		
		assertTrue("Delivery Man should have a currentDelivery in it. It doesn't.", deliveryMan.getCurrentDelivery() != null);
		assertTrue("Current delivery requested items size should equal 1. It doesn't.", deliveryMan.getCurrentDelivery().requestedItems.size() == 1);
		assertTrue("Current delivery requested items amounts size should equal 1. It doesn't.", deliveryMan.getCurrentDelivery().requestedItemsAmounts.size() == 1);
		assertTrue("Delivery Man 2 should have a currentDelivery in it. It doesn't.", deliveryMan2.getCurrentDelivery() != null);
		assertTrue("Current delivery requested items size should equal 1. It doesn't.", deliveryMan2.getCurrentDelivery().requestedItems.size() == 1);
		assertTrue("Current delivery requested items amounts size should equal 1. It doesn't.", deliveryMan2.getCurrentDelivery().requestedItemsAmounts.size() == 1);

		// Step 2 - Delivery Man and Delivery Man 2 call scheduler and go to respective restaurants
		deliveryMan.atExit.release();
		deliveryMan.atDestination.release();
		deliveryMan.atDestination.release();
		deliveryMan.atDestination.release();
		deliveryMan2.atExit.release();
		deliveryMan2.atDestination.release();
		deliveryMan2.atDestination.release();
		deliveryMan2.atDestination.release();
		assertTrue("Delivery Man's scheduler should return true (next action is to go to restaurant), but didn't.", 
				deliveryMan.pickAndExecuteAnAction());
		assertTrue("Delivery Man2's scheduler should return true (next action is to go to restaurant), but didn't.", 
				deliveryMan2.pickAndExecuteAnAction());
		assertTrue("Delivery Man's GUI X Destination should be 0, but isn't.", dmGUI.xDestination == 0);
		assertTrue("Delivery Man's GUI Y Destination should be 0, but isn't.", dmGUI.yDestination == 0);
		assertFalse("Delivery Man's isPresent should be false, but isn't.", dmGUI.isPresent());
		assertFalse("Delivery Man's SimCityGUI isPresent should be false.", simCityGUI.isPresent());
		assertTrue("Delivery Man's City GUI X Destination should be 100, but isn't.", simCityGUI.xDestination == 100);
		assertTrue("Delivery Man's City GUI Y Destination should be 100, but isn't.", simCityGUI.yDestination == 100);
		assertTrue("Cashier should have logged \"Received DeliveryItemsAboutToBeDelivered\". Instead his log reads: " +
				marketCashier.log.getLastLoggedEvent().toString(), marketCashier.log.containsString("Received DeliveryItemsAboutToBeDelivered"));
		assertTrue("Delivery Man2's GUI X Destination should be 0, but isn't.", dmGUI2.xDestination == 0);
		assertTrue("Delivery Man2's GUI Y Destination should be 0, but isn't.", dmGUI2.yDestination == 0);
		assertFalse("Delivery Man2's isPresent should be false, but isn't.", dmGUI2.isPresent());
		assertFalse("Delivery Man2's SimCityGUI isPresent should be false.", simCityGUI2.isPresent());
		assertTrue("Delivery Man2's City GUI X Destination should be 200, but isn't.", simCityGUI2.xDestination == 200);
		assertTrue("Delivery Man2's City GUI Y Destination should be 200, but isn't.", simCityGUI2.yDestination == 200);

		// Step 3 - Delivery Man gives cook of restaurant the order
		assertTrue("Cook should have logged \"Received HereIsDelivery\". Instead his log reads: " +
				cook.log.getLastLoggedEvent().toString(), cook.log.containsString("Received HereIsDelivery"));
		assertTrue("Restaurant Cashier should have logged \"Received NeedDeliveryPayment\". Instead his log reads: " +
				restaurantCashier.log.getLastLoggedEvent().toString(), restaurantCashier.log.containsString("Received NeedDeliveryPayment"));
		
		// Step 4 - Restaurant Cashier gives payment to Delivery Men
		deliveryMan.HereIsDeliveryPayment(6.00);
		assertTrue("Delivery Payment should be 6.00", deliveryMan.getDeliveryPayment() == 6.00);
		deliveryMan2.HereIsDeliveryPayment(2.00);
		assertTrue("Delivery Payment should be 8.00", deliveryMan2.getDeliveryPayment() == 2.00);

		// Step 5 - Delivery Men call scheduler and leave restaurants
		deliveryMan.atDestination.release();
		deliveryMan2.atDestination.release();
		deliveryMan.atDestination.release();
		deliveryMan2.atDestination.release();
		deliveryMan.atDestination.release();
		deliveryMan2.atDestination.release();
		assertTrue("Delivery Man's scheduler should return true (next action is to leave restaurant), but didn't.", 
				deliveryMan.pickAndExecuteAnAction());
		assertTrue("Delivery Man2's scheduler should return true (next action is to leave restaurant), but didn't.", 
				deliveryMan2.pickAndExecuteAnAction());
		assertTrue("Current Delivery should be null", deliveryMan.getCurrentDelivery() == null);
		assertTrue("Delivery Man GUI should be present", dmGUI.isPresent());
		assertFalse("Delivery Man SimCity GUI should not be present", simCityGUI.isPresent());
		assertTrue("Current Delivery should be null", deliveryMan2.getCurrentDelivery() == null);
		assertTrue("Delivery Man GUI should be present", dmGUI2.isPresent());
		assertFalse("Delivery Man SimCity GUI should not be present", simCityGUI2.isPresent());
		
		// Step 6 - Delivery Men call scheduler and give payment to Market Cashier
		deliveryMan.atCashier.release();
		deliveryMan2.atCashier.release();
		assertTrue("Delivery Man's scheduler should return true (next action is to give payment to Cashier), but didn't.", 
				deliveryMan.pickAndExecuteAnAction());
		assertTrue("Delivery Man2's scheduler should return true (next action is to give payment to Cashier), but didn't.", 
				deliveryMan2.pickAndExecuteAnAction());
		assertTrue("Delivery Man's GUI X Destination should be 450, but isn't.", dmGUI.xDestination == 450);
		assertTrue("Delivery Man's GUI Y Destination should be 200, but isn't.", dmGUI.yDestination == 200);
		assertTrue("Delivery Payment should be 0", deliveryMan2.getDeliveryPayment() == 0);
		assertTrue("Delivery Man2's GUI X Destination should be 450, but isn't.", dmGUI2.xDestination == 450);
		assertTrue("Delivery Man2's GUI Y Destination should be 200, but isn't.", dmGUI2.yDestination == 200);
		assertTrue("Market Cashier should have logged \"Received HereIsPayment for $2.0\". Instead his log reads: " +
				marketCashier.log.getLastLoggedEvent().toString(), marketCashier.log.containsString("Received HereIsPayment for $2.0"));
		assertTrue("Getter should have logged \"Received IAmAvailable\". Instead his log reads: " +
				getter.log.getLastLoggedEvent().toString(), getter.log.containsString("Received IAmAvailable"));
		assertTrue("Delivery Payment should be 0", deliveryMan2.getDeliveryPayment() == 0);

		// Step 7 - No further action
		assertFalse("Delivery Man's scheduler should return false (no actions remain), but didn't.", 
				deliveryMan.pickAndExecuteAnAction());
		assertFalse("Delivery Man2's scheduler should return false (no actions remain), but didn't.", 
				deliveryMan2.pickAndExecuteAnAction());
	}

	// Test Four - One Delivery Man fulfills DO (two items), then comes back and fulfills another DO (one item)
	public void testFour_DeliveryMan_Normative(){
		// Preconditions
		assertEquals("DeliveryMan should have $0.00 in Delivery Payment. It doesn't.", deliveryMan.getDeliveryPayment(), 0.00);
		assertEquals("DeliveryMan should have an empty event log before HereAreItemsForDelivery is called. "
				+ "Instead, the Cashier's event log reads: " + deliveryMan.log.toString(), 0, deliveryMan.log.size());

		// Step 1 - DeliveryMan gets message from Cashier saying delivery is needed
		List<String> food = new ArrayList<String>();
		food.add("Chicken");
		food.add("Onion");
		List<Integer> amount = new ArrayList<Integer>();
		amount.add(2);
		amount.add(4);
		DeliveryOrder order = new DeliveryOrder(food, amount, restaurant, marketCashier, 10.00);
		order.state = DeliveryOrderState.Retrieved;
		deliveryMan.HereIsOrderToBeDelivered(order);
		assertTrue("Delivery Man should have a currentDelivery in it. It doesn't.", deliveryMan.getCurrentDelivery() != null);
		assertTrue("Current delivery requested items size should equal 2. It doesn't.", deliveryMan.getCurrentDelivery().requestedItems.size() == 2);
		assertTrue("Current delivery requested items amounts size should equal 2. It doesn't.", deliveryMan.getCurrentDelivery().requestedItemsAmounts.size() == 2);

		// Step 2 - Delivery Man calls scheduler and goes to restaurant
		deliveryMan.atExit.release();
		deliveryMan.atDestination.release();
		deliveryMan.atDestination.release();
		deliveryMan.atDestination.release();
		assertTrue("Delivery Man's scheduler should return true (next action is to go to restaurant), but didn't.", 
				deliveryMan.pickAndExecuteAnAction());
		assertTrue("Cashier should have logged \"Received DeliveryItemsAboutToBeDelivered\". Instead his log reads: " +
				marketCashier.log.getLastLoggedEvent().toString(), marketCashier.log.containsString("Received DeliveryItemsAboutToBeDelivered"));
		assertTrue("Delivery Man's GUI X Destination should be -20, but isn't.", dmGUI.xDestination == 0);
		assertTrue("Delivery Man's GUI Y Destination should be -20, but isn't.", dmGUI.yDestination == 0);
		assertFalse("Delivery Man's isPresent should be false, but isn't.", dmGUI.isPresent());
		assertFalse("Delivery Man's SimCityGUI isPresent should be false.", simCityGUI.isPresent());
		assertTrue("Delivery Man's City GUI X Destination should be 100, but isn't.", simCityGUI.xDestination == 100);
		assertTrue("Delivery Man's City GUI Y Destination should be 100, but isn't.", simCityGUI.yDestination == 100);

		// Step 3 - Delivery Man gives cook of restaurant the order
		assertTrue("Cook should have logged \"Received HereIsDelivery\". Instead his log reads: " +
				cook.log.getLastLoggedEvent().toString(), cook.log.containsString("Received HereIsDelivery"));
		assertTrue("Restaurant Cashier should have logged \"Received NeedDeliveryPayment\". Instead his log reads: " +
				restaurantCashier.log.getLastLoggedEvent().toString(), restaurantCashier.log.containsString("Received NeedDeliveryPayment"));

		// Step 4 - Restaurant Cashier gives payment to Delivery Man
		deliveryMan.HereIsDeliveryPayment(8.00);
		assertTrue("Delivery Payment should be 8.00", deliveryMan.getDeliveryPayment() == 8.00);

		// Step 5 - Delivery Man calls scheduler and leaves restaurant
		deliveryMan.atDestination.release();
		deliveryMan.atDestination.release();
		deliveryMan.atDestination.release();
		assertTrue("Delivery Man's scheduler should return true (next action is to leave restaurant), but didn't.", 
				deliveryMan.pickAndExecuteAnAction());
		assertTrue("Current Delivery should be null", deliveryMan.getCurrentDelivery() == null);
		assertTrue("Delivery Man GUI should be present", dmGUI.isPresent());
		assertFalse("Delivery Man SimCity GUI should not be present", simCityGUI.isPresent());

		// Step 6 - Delivery Man calls scheduler and gives payment to Market Cashier
		deliveryMan.atCashier.release();
		assertTrue("Delivery Man's scheduler should return true (next action is to give payment to Cashier), but didn't.", 
				deliveryMan.pickAndExecuteAnAction());
		assertTrue("Delivery Man's GUI X Destination should be 450, but isn't.", dmGUI.xDestination == 450);
		assertTrue("Delivery Man's GUI Y Destination should be 200, but isn't.", dmGUI.yDestination == 200);
		assertTrue("Market Cashier should have logged \"Received HereIsPayment for $8.0\". Instead his log reads: " +
				marketCashier.log.getLastLoggedEvent().toString(), marketCashier.log.containsString("Received HereIsPayment for $8.0"));
		assertTrue("Getter should have logged \"Received IAmAvailable\". Instead his log reads: " +
				getter.log.getLastLoggedEvent().toString(), getter.log.containsString("Received IAmAvailable"));
		assertTrue("Delivery Payment should be 0", deliveryMan.getDeliveryPayment() == 0);

		// Step 7 - No further action
		assertFalse("Delivery Man's scheduler should return false (no actions remain), but didn't.", 
				deliveryMan.pickAndExecuteAnAction());		
		assertEquals("DeliveryMan should have $0.00 in Delivery Payment. It doesn't.", deliveryMan.getDeliveryPayment(), 0.00);

		// Step 8 - DeliveryMan gets message from Cashier saying delivery is needed
		List<String> food2 = new ArrayList<String>();
		food2.add("Microwave");
		List<Integer> amount2 = new ArrayList<Integer>();
		amount2.add(1);
		DeliveryOrder order2 = new DeliveryOrder(food2, amount2, restaurant2, marketCashier, 10.00);
		order2.state = DeliveryOrderState.Retrieved;
		deliveryMan.HereIsOrderToBeDelivered(order2);
		assertTrue("Delivery Man should have a currentDelivery in it. It doesn't.", deliveryMan.getCurrentDelivery() != null);
		assertTrue("Current delivery requested items size should equal 1. It doesn't.", deliveryMan.getCurrentDelivery().requestedItems.size() == 1);
		assertTrue("Current delivery requested items amounts size should equal 1. It doesn't.", deliveryMan.getCurrentDelivery().requestedItemsAmounts.size() == 1);

		// Step 9 - Delivery Man calls scheduler and goes to restaurant
		deliveryMan.atExit.release();
		deliveryMan.atDestination.release();
		deliveryMan.atDestination.release();
		deliveryMan.atDestination.release();
		assertTrue("Delivery Man's scheduler should return true (next action is to go to restaurant), but didn't.", 
				deliveryMan.pickAndExecuteAnAction());
		assertTrue("Cashier should have logged \"Received DeliveryItemsAboutToBeDelivered\". Instead his log reads: " +
				marketCashier.log.getLastLoggedEvent().toString(), marketCashier.log.containsString("Received DeliveryItemsAboutToBeDelivered"));
		assertTrue("Delivery Man's GUI X Destination should be 0, but isn't.", dmGUI.xDestination == 0);
		assertTrue("Delivery Man's GUI Y Destination should be 0, but isn't.", dmGUI.yDestination == 0);
		assertFalse("Delivery Man's isPresent should be false, but isn't.", dmGUI.isPresent());
		assertFalse("Delivery Man's SimCityGUI isPresent should be false.", simCityGUI.isPresent());
		assertTrue("Delivery Man's City GUI X Destination should be 100, but isn't.", simCityGUI.xDestination == 200);
		assertTrue("Delivery Man's City GUI Y Destination should be 100, but isn't.", simCityGUI.yDestination == 200);

		// Step 10 - Delivery Man gives cook of restaurant the order
		assertTrue("Cook should have logged \"Received HereIsDelivery\". Instead his log reads: " +
				cook.log.getLastLoggedEvent().toString(), cook.log.containsString("Received HereIsDelivery"));
		assertTrue("Restaurant Cashier should have logged \"Received NeedDeliveryPayment\". Instead his log reads: " +
				restaurantCashier.log.getLastLoggedEvent().toString(), restaurantCashier.log.containsString("Received NeedDeliveryPayment"));

		// Step 11 - Restaurant Cashier gives payment to Delivery Man
		deliveryMan.HereIsDeliveryPayment(20.00);
		assertTrue("Delivery Payment should be 20.00", deliveryMan.getDeliveryPayment() == 20.00);

		// Step 12 - Delivery Man calls scheduler and leaves restaurant
		deliveryMan.atDestination.release();
		deliveryMan.atDestination.release();
		deliveryMan.atDestination.release();
		assertTrue("Delivery Man's scheduler should return true (next action is to leave restaurant), but didn't.", 
				deliveryMan.pickAndExecuteAnAction());
		assertTrue("Current Delivery should be null", deliveryMan.getCurrentDelivery() == null);
		assertTrue("Delivery Man GUI should be present", dmGUI.isPresent());
		assertFalse("Delivery Man SimCity GUI should not be present", simCityGUI.isPresent());

		// Step 13 - Delivery Man calls scheduler and gives payment to Market Cashier
		deliveryMan.atCashier.release();
		assertTrue("Delivery Man's scheduler should return true (next action is to give payment to Cashier), but didn't.", 
				deliveryMan.pickAndExecuteAnAction());
		assertTrue("Delivery Man's GUI X Destination should be 450, but isn't.", dmGUI.xDestination == 450);
		assertTrue("Delivery Man's GUI Y Destination should be 200, but isn't.", dmGUI.yDestination == 200);
		assertTrue("Market Cashier should have logged \"Received HereIsPayment for $20.0\". Instead his log reads: " +
				marketCashier.log.getLastLoggedEvent().toString(), marketCashier.log.containsString("Received HereIsPayment for $20.0"));
		assertTrue("Getter should have logged \"Received IAmAvailable\". Instead his log reads: " +
				getter.log.getLastLoggedEvent().toString(), getter.log.containsString("Received IAmAvailable"));
		assertTrue("Delivery Payment should be 0", deliveryMan.getDeliveryPayment() == 0);

		// Step 14 - No further action
		assertFalse("Delivery Man's scheduler should return false (no actions remain), but didn't.", 
				deliveryMan.pickAndExecuteAnAction());
	}
	
	// Test Five - Delivery Man attempts delivery, but restaurant is closed. Redelivers when restaurant is open.
}
