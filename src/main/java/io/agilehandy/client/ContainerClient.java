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

import io.agilehandy.command.api.ContainerBookCommand;
import io.agilehandy.command.api.ContainerCreateCommand;
import io.agilehandy.command.api.ContainerOpCommand;
import io.agilehandy.command.api.ContainerTransitCommand;
import io.agilehandy.command.api.OpStatus;
import io.agilehandy.command.api.TransitStatus;
import io.agilehandy.command.api.TransitType;
import io.agilehandy.command.impl.Container;
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

	public CommandLineRunner clr() {
		return args -> {
			// create 5 containers
			List<Container> containers = new ArrayList<>();
			for (int i = 0; i< 6; i++) {
				ContainerCreateCommand cmd = new ContainerCreateCommand(UUID.randomUUID(),
								100f, "zone-1", "port-1");
				containers.add(new Container(cmd));
			}
			// book 2 containers
			Container container1 = containers.get(new Random().nextInt(containers.size()));
			Container container2 = containers.get(new Random().nextInt(containers.size()));

			ContainerBookCommand bookCommand1 = new ContainerBookCommand(container1.getId(),
					UUID.randomUUID(), TransitType.PRIORITY, "zone-3", "port-3");

			ContainerBookCommand bookCommand2 = new ContainerBookCommand(container2.getId(),
					UUID.randomUUID(), TransitType.STANDARD,"zone-5", "port-5");

			container1.book(bookCommand1);
			container2.book(bookCommand2);

			// start loading container 1
			ContainerOpCommand opCommand = new ContainerOpCommand(container1.getId(),
					OpStatus.LOADING_STARTED, 80f);
			container1.operate(opCommand);

			// finish loading container 1
			opCommand = new ContainerOpCommand(container1.getId(),
					OpStatus.LOADING_COMPLETED, container1.getUsedSize());
			container1.operate(opCommand);

			// transit container 1
			ContainerTransitCommand transitCommand = new ContainerTransitCommand(container1.getId(),
					TransitStatus.TRANSIT, LocalDateTime.now() );
			container1.transit(transitCommand);
		};
	}
}
