package carDealership;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;

import carDealership.gui.CarDealerGui;
import simCity.CarDealership;
import simCity.PersonAgent;
import vehicles.agents.Car;
import agent.Role;

public class CarDealerRole extends Role {
	// Data
	private CarDealership dealership;
	private PersonAgent person;

	private MyCarCustomer currentCustomer;
	
	enum MyCustomerState {None, GavePreferences, GivenSuitableCars, ChoseCar, AskedForPayment, Paid, 
		GivenKeys};

	// Animation Semaphore
	public Semaphore waitForAnimationSemaphore = new Semaphore(0, true);

	public CarDealerRole(PersonAgent pa, CarDealership dealership) {
		super(pa);
		person = pa;
		this.dealership = dealership;
	}

	// Messages
	public void HereAreMyPreferences(HashMap<String, String> preferences){ // From Car Customer
		currentCustomer.preferences = preferences;
		currentCustomer.state = MyCustomerState.GavePreferences;
		stateChanged();
	}
	
	public void IChooseThisCar(Car chosenCar){ // From Car Customer
		currentCustomer.chosenCar = chosenCar;
//		currentCustomer.carPayment = chosenCar.getPrice();
		currentCustomer.state = MyCustomerState.ChoseCar;
		stateChanged();
	}
	
	public void HereIsCarPayment(double payment){
		currentCustomer.state = MyCustomerState.Paid;
		stateChanged();
	}

	public void AnimationRelease(){ // From GUI
		waitForAnimationSemaphore.release();
		stateChanged();
	}

	// Scheduler
	public boolean pickAndExecuteAnAction() {
		if (currentCustomer == null && dealership.getWaitingCustomers().size() > 0){
			helpCurrentCustomer();
			return true;
		}
		if (currentCustomer.state == MyCustomerState.GavePreferences){
			pickSuitableCars();
			currentCustomer.state = MyCustomerState.GivenSuitableCars;
			return true;
		}
		if (currentCustomer.state == MyCustomerState.ChoseCar){
			getCarPayment();
			currentCustomer.state = MyCustomerState.AskedForPayment;
			return true;
		}
		if (currentCustomer.state == MyCustomerState.Paid){
			giveKeysToCustomer();
			currentCustomer.state = MyCustomerState.GivenKeys;
			return true;
		}
//		dealerGui.DoGoToDefaultPosition();
		return false;
	}

	// Actions
	private void helpCurrentCustomer(){
		currentCustomer = new MyCarCustomer(dealership.getWaitingCustomers().get(0));
		dealership.getWaitingCustomers().remove(currentCustomer);

//		dealerGui.DoGoToCustomer(currentCustomer.customer);
		waitForAnimation();

		currentCustomer.customer.HowCanIHelpYou(this);
	}
	
	private void pickSuitableCars(){		
//		dealerGui.DoGoToDesk();
		waitForAnimation();
		
		// Stub - go through dealership.inventory and find suitable cars
		
//		dealerGui.DoGoToCustomer(currentCustomer.customer);
		
//		currentCustomer.customer.HereAreSuitableCars(suitableCars);
	}
	
	private void getCarPayment(){
		double payment = currentCustomer.carPayment;
		
//		dealerGui.SayNeedPayment(payment);
		
		currentCustomer.customer.NeedCarPayment(payment);
	}
	
	private void giveKeysToCustomer(){
//		dealerGui.SayHereAreKeys();
		// Stub - remove car from dealership inventory
		
		currentCustomer.customer.HereAreTheKeys();
		
		currentCustomer = null;
	}

	// Utilities
	public void waitForAnimation(){
		try {
			waitForAnimationSemaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void setGui(CarDealerGui gui){
		super.setGui(gui);
	}
	
	public CarDealerGui getGui() {
		return (CarDealerGui)super.getGui();
	}

	class MyCarCustomer{
		CarCustomerRole customer;
		HashMap<String, String> preferences = new HashMap<String, String>();
		List<Car> suitableCars = new ArrayList<Car>();
		Car chosenCar;
		double carPayment;
		MyCustomerState state;

		public MyCarCustomer(CarCustomerRole c){
			customer = c;
			state = MyCustomerState.None;
		}
	}
}
