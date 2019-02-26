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


package io.agilehandy.query.inventory;

import io.agilehandy.command.api.evt.ContainerCreated;
import io.agilehandy.command.api.evt.ContainerOpReserved;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author Haytham Mohamed
 **/
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryProjection {

	private final InventoryRepository repository;

	// when creating a new container
	@EventHandler
	public void on(ContainerCreated event) {
		log.debug("projecting {}", event);
		InventoryKey key = new InventoryKey();
		key.setZoneName(event.getCurrentZoneName());
		key.setPortName(event.getCurrentPortName());

		Inventory inventory = new Inventory();
		inventory.setKey(key);
		inventory.setAvailableContainers(1);
		inventory.setLastUpdated(LocalDateTime.now());
		repository.save(inventory);
	}


	// When reserving a container update origin zone/port available containers
	@EventHandler
	public void onOrigin(ContainerOpReserved event) {
		log.debug("projecting {}", event);
		Inventory originInventory = repository.findById(
				new InventoryKey(event.getOrigZoneName(), event.getOrigPortName())
		).orElseGet(() -> null);

		if (originInventory != null && originInventory.getAvailableContainers() > 0) {
			originInventory.setAvailableContainers(originInventory.getAvailableContainers() - 1);
			repository.save(originInventory);
		}
	}

	// When reserving a container update destination zone/port forecast containers
	@EventHandler
	public void onDest(ContainerOpReserved event) {
		log.debug("projecting {}", event);
		Inventory destInventory = repository.findById(
				new InventoryKey(event.getDestZoneName(), event.getDestPortName())
		).orElseGet(() -> null);
		if (destInventory != null) {
			destInventory.setForecastContainers(destInventory.getForecastContainers() + 1);
			repository.save(destInventory);
		}
	}

	@QueryHandler
	public Inventory getAvailableInventory(InventoryQuery query) {
		return repository.findById(new InventoryKey(query.getZoneName(), query.getPortName()))
				.orElse(null);
	}

}

