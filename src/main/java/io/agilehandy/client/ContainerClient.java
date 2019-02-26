/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.agilehandy.client;

import io.agilehandy.command.api.TransmitType;
import io.agilehandy.command.api.cmd.ContainerCreateCommand;
import io.agilehandy.command.api.cmd.ContainerOpLoadCommand;
import io.agilehandy.command.api.cmd.ContainerOpOffLoadCommand;
import io.agilehandy.command.api.cmd.ContainerOpReleasedCommand;
import io.agilehandy.command.api.cmd.ContainerOpReserveCommand;
import io.agilehandy.command.api.cmd.ContainerTransArriveCommand;
import io.agilehandy.command.api.cmd.ContainerTransBoardCommand;
import io.agilehandy.command.api.cmd.ContainerTransDepartCommand;
import io.agilehandy.command.api.cmd.ContainerTransOffBoardCommand;
import io.agilehandy.query.inventory.Inventory;
import io.agilehandy.query.inventory.InventoryQuery;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * @author Haytham Mohamed
 **/
@Component
@Slf4j
public class ContainerClient implements CommandLineRunner {

	private final CommandGateway commandGateway;
	private final QueryGateway queryGateway;

	public ContainerClient(CommandGateway commandGateway, QueryGateway queryGateway) {
		this.commandGateway = commandGateway;
		this.queryGateway = queryGateway;
	}

	public void run(String... args) {
		log.info("==> start");
		// create 5 containers
		List<UUID> Ids = new ArrayList<>();
		for (int i = 0; i< 6; i++) {
			ContainerCreateCommand cmd = new ContainerCreateCommand(UUID.randomUUID(),
					new Random().nextInt(6) * 100f, "zone-1", "port-1");

			Ids.add(commandGateway.sendAndWait(cmd));
		}

		// pick a container
		log.info("Picking any container");
		UUID id = Ids.get(new Random().nextInt(Ids.size()));

		// reserve a container
		log.info("reserve a container");
		ContainerOpReserveCommand reserveCommand = new ContainerOpReserveCommand(id,
				UUID.randomUUID(), TransmitType.PRIORITY, "zone-2", "port-2");
		commandGateway.sendAndWait(reserveCommand);

		// load container
		log.info("load a container");
		commandGateway.sendAndWait(new ContainerOpLoadCommand(id, 80f));

		// board container to a ship
		log.info("board a container");
		commandGateway.sendAndWait(new ContainerTransBoardCommand(id));

		// depart container from origin
		log.info("depart a container");
		commandGateway.sendAndWait(new ContainerTransDepartCommand(id));

		// arrive container to destination
		log.info("arrive a container");
		commandGateway.sendAndWait(new ContainerTransArriveCommand(id));

		// off board container at destination
		log.info("off board a container");
		commandGateway.sendAndWait(new ContainerTransOffBoardCommand(id));

		// off load container
		log.info("off load a container");
		commandGateway.sendAndWait(new ContainerOpOffLoadCommand(id));

		// release container
		log.info("release a container");
		commandGateway.send(new ContainerOpReleasedCommand(id));

		// because of eventual consistency, let's pause a little
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// query zone/port
		log.info("querying");
		InventoryQuery query1 = new InventoryQuery("zone-1", "port-1");
		InventoryQuery query2 = new InventoryQuery("zone-2", "port-2");

		Inventory inv1 = queryGateway.query(query1
				, ResponseTypes.instanceOf(Inventory.class)).join();
		Inventory inv2 = queryGateway.query(query2
				, ResponseTypes.instanceOf(Inventory.class)).join();

		log.info("inventory zone-1/port-1 : " + inv1);
		log.info("inventory zone-2/port-2 : " + inv2);

		log.info("==> finish");
	}

}
