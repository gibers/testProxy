package com.sandbox.testproxy;

interface CustomerService {

    @TestProxyApplication.MyTransactional
    void createP();

    void addP();

}
