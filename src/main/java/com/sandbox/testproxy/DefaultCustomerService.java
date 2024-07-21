package com.sandbox.testproxy;

public class DefaultCustomerService implements CustomerService {

    @Override
    public void createP() {
        System.out.println("create()");
    }

    @Override
    public void addP() {
        System.out.println("addP()");
    }


}
