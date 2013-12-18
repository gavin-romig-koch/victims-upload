package com.redhat.cloudconsole.api.persistence.dao;

import com.mongodb.DBObject;
import com.redhat.cloudconsole.api.persistence.entity.Machine;
import com.redhat.cloudconsole.api.resources.CloudCollectionsEnum;
import com.redhat.cloudconsole.api.resources.MongoProvider;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;


@Stateless
@LocalBean
public class AccountDao {

    @EJB
    private MongoProvider mongoProvider;

    /**
     * Return a list of machine json objects that have the specified accountId
     * @param accountId
     * @return
     * @throws Exception
     */
    public Iterable<Machine> getAccount(String accountId) throws Exception {
        // TODO may want to project the data where accountId is the top level object
//        mongoProvider.getJongoCollection(CloudCollectionsEnum.MACHINES)
//                .aggregate("{$match: {accountId: #}}", accountId)
//                .and("{$group: {_id: {}}}");
        return mongoProvider.getMachinesColl().find("{accountId: #}", accountId).as(Machine.class);
    }


//
//    @TransactionAttribute(TransactionAttributeType.REQUIRED)
//    public void addAccount(Account account) {
//        em.persist(account);
//    }
//
//    @TransactionAttribute(TransactionAttributeType.REQUIRED)
//    public void deleteAccount(String accountId) {
//        Account a = em.find(Account.class, accountId);
//        em.remove(a);
//    }


    /*
     * From an upstream perspective the machines in an account need the context of the account.  Therefore this really
     * just returns the account with the machines embedded.
     *
     * The main point of this explicit method is it ensures the machines are eagerly fetched always
     */
//    @TransactionAttribute(TransactionAttributeType.REQUIRED)
//    public Account getMachines(String accountId) {
//        Query query = em.createQuery("SELECT a FROM Account a join fetch a.machines WHERE a.accountId = :accountId");
//        query.setParameter("accountId", accountId);
//        return (Account)query.getSingleResult();
//    }

//    @TransactionAttribute(TransactionAttributeType.REQUIRED)
//    public void addMachine(String accountId, String uuid) {
//        Machine m = new Machine();
//        m.setUUID(uuid);
//
//        Account a = em.find(Account.class, accountId);
//        //Cascade.ALL implies a persist of the machine at the transaction boundary
//        a.addMachine(m);
//    }
//
//    @TransactionAttribute(TransactionAttributeType.REQUIRED)
//    public void deleteMachine(String accountId, String uuid) {
//        Machine m = new Machine();
//        m.setMachineId(uuid);
//
//        Account a = em.find(Account.class, accountId);
//        a.deleteMachine(m);

        //Cascade.ALL implies a remove of the machine at the transaction boundary
//    }

}
