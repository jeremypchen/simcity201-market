package carDealership;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;

import carDealership.gui.CarCustomerGui;
import simCity.CarDealership;
import simCity.PersonAgent;
import vehicles.agents.Car;
import agent.Role;

public class CarCustomerRole extends Role {
	// Data
	private CarDealerRole dealer;
	private CarDealership dealership;
	private PersonAgent person;

	private HashMap<String, String> preferences = new HashMap<String,String>();
	private List<Car> suitableCars = new ArrayList<Car>();
	private Car chosenCar;
	private double carPayment;

	// Animation Semaphore
	public Semaphore waitForAnimationSemaphore = new Semaphore(0, true);

	enum CarCustomerState {None, AtDealership, WaitingToGivePreferences, AtFront, GavePreferences,
		GivenPreferences, ChoseCar, NeedToPay, PaidDealer, GivenKeys, Leaving, Left, NoPreferredCar};
		CarCustomerState state = CarCustomerState.None;

		public CarCustomerRole(PersonAgent pa, CarDealership dealership) {
			super(pa);
			person = pa;
			this.dealership = dealership;
			state = CarCustomerState.AtDealership;
		}

		// Messages
		public void HowCanIHelpYou(CarDealerRole cd){ // From Car Dealer
			dealer = cd;
			state = CarCustomerState.AtFront;
			stateChanged();
		}

		public void HereAreSuitableCars(List<Car> suitableCars){ // From Car Dealer
			this.suitableCars = suitableCars;
			state = CarCustomerState.GivenPreferences;
			stateChanged();
		}

		public void NeedCarPayment(double payment){ // From Car Dealer
			carPayment = payment;
			state = CarCustomerState.NeedToPay;
			stateChanged();
		}
		
		public void HereAreTheKeys(){ // From Car Dealer
			state = CarCustomerState.GivenKeys;
			stateChanged();
		}
		
		public void AnimationRelease(){ // From GUI
			waitForAnimationSemaphore.release();
			stateChanged();
		}

		// Scheduler
		public boolean pickAndExecuteAnAction() {
			if (state == CarCustomerState.AtDealership){
				goToWaitingArea();
				state = CarCustomerState.WaitingToGivePreferences;
				return true;
			}
			if (state == CarCustomerState.AtFront){
				giveCarPreferences();
				state = CarCustomerState.GavePreferences;
				return true;
			}
			if (state == CarCustomerState.GivenPreferences){
				chooseCar();
				state = CarCustomerState.ChoseCar;
				return true;
			}
			if (state == CarCustomerState.NeedToPay){
				payDealer();
				state = CarCustomerState.PaidDealer;
				return true;
			}
			if (state == CarCustomerState.GivenKeys){
				getCarAndLeave();
				state = CarCustomerState.Leaving;
				return true;
			}
			if (state == CarCustomerState.Leaving){
				returnToCity();
				state = CarCustomerState.Left;
				return true;
			}
			return false;
		}

		// Actions
		private void goToWaitingArea(){
			print("Here are dealership to get a car");
			getGui().DoEnterDealership();
			waitForAnimation();
			dealership.HereToBuyCar(this);
		}

		private void giveCarPreferences(){
			getGui().DoGoToDealer();
			waitForAnimation();
			getGui().SayPreferences(preferences);
			dealer.HereAreMyPreferences(preferences);
		}

		private void chooseCar(){
			int random = 0; // generate random number
			chosenCar = suitableCars.get(random);
			getGui().SayChosenCar(chosenCar);
			dealer.IChooseThisCar(chosenCar);
		}

		private void payDealer(){
			getGui().SayPayment(carPayment);
			if (person.cashOnHand >= carPayment) {
				person.cashOnHand -= carPayment;
				dealer.HereIsCarPayment(carPayment);
			} 
		}
		
		private void getCarAndLeave(){
			getGui().DoGoToNewCar(chosenCar);
			waitForAnimation();
			dealership.Leaving(this);
		}
		
		private void returnToCity(){
			getPersonAgent().msgReturnToCity();
			setActive(false);
			getGui().setPresent(false);
		}

		// Utilities
		public void waitForAnimation(){
			try {
				waitForAnimationSemaphore.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		public void setGui(CarCustomerGui gui){
			super.setGui(gui);
		}
		
		public CarCustomerGui getGui() {
			return (CarCustomerGui)super.getGui();
		}

		public void setCarPreferences(String make, String model, double price){
			preferences.put("Make", make);
			preferences.put("Model", model);
			preferences.put("Price", "" + price);
		}

}
