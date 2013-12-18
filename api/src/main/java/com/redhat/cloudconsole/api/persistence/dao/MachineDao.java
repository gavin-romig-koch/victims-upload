package com.redhat.cloudconsole.api.persistence.dao;

import com.redhat.cloudconsole.api.persistence.entity.Machine;
import com.redhat.cloudconsole.api.resources.MongoProvider;

import javax.ejb.*;
import java.util.List;

@Stateless
@LocalBean
public class MachineDao {

    @EJB
    private MongoProvider mongoProvider;

    public List<Machine> getMachines() {
        return null;
    }

    public Machine getMachine(String uuid) throws Exception {
        return mongoProvider.getMachinesColl().findOne("{_id: #}", uuid).as(Machine.class);
    }

    public void registerMachine(Machine machine) throws Exception {
        mongoProvider.getMachinesColl().save(machine);
    }

    public void deleteMachine(String uuid) throws Exception {
        // Remove all in the machines collection with the _id
        mongoProvider.getMachinesColl().remove("{_id: #}", uuid);

        // Remove all in the metrics collection with the uuid
        mongoProvider.getMachinesColl().remove("{uuid: #}", uuid);
    }
}
