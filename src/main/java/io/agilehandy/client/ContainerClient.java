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

import io.agilehandy.command.api.ContainerReserveCommand;
import io.agilehandy.command.api.ContainerCreateCommand;
import io.agilehandy.command.api.ContainerOpCommand;
import io.agilehandy.command.api.ContainerTransmitCommand;
import io.agilehandy.command.api.TransmitType;
import io.agilehandy.command.impl.Container;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * @author Haytham Mohamed
 **/
@Component
public class ContainerClient {

	private final CommandGateway commandGateway;

	public ContainerClient(CommandGateway commandGateway) {
		this.commandGateway = commandGateway;
	}

	public CommandLineRunner clr() {
		return args -> {
			// create 5 containers
			List<Container> containers = new ArrayList<>();
			for (int i = 0; i< 6; i++) {
				ContainerCreateCommand cmd = new ContainerCreateCommand(UUID.randomUUID(),
								new Random().nextInt(6) * 100f, "zone-1", "port-1");

				containers.add(commandGateway.sendAndWait(cmd));
			}

			// pick a container
			Container container = containers.get(new Random().nextInt(containers.size()));

			// reserve a container
			ContainerReserveCommand bookCommand = new ContainerReserveCommand(container.getId(),
					UUID.randomUUID(), TransmitType.PRIORITY, "zone-3", "port-3");

			container.reserve(bookCommand);


			// load container
			ContainerOpCommand opCommand = new ContainerOpCommand(container.getId(), 80f);

			container.load(opCommand);

			ContainerTransmitCommand transmitCommand =
					new ContainerTransmitCommand(container.getId(), LocalDateTime.now() );

			// board container to a ship
			container.board(transmitCommand);

			// depart container from origin
			container.depart(transmitCommand);

			// arrive container to destination
			container.arrive(transmitCommand);

			// off board container from a ship
			container.offBoard(transmitCommand);

			// start offloading container
			container.offLoad(opCommand);

			// release container
			container.release(opCommand);
		};
	}
}
