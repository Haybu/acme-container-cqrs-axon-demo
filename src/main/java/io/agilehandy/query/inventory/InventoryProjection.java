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

import io.agilehandy.command.api.ContainerCreated;
import io.agilehandy.command.api.ContainerReserved;
import lombok.RequiredArgsConstructor;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author Haytham Mohamed
 **/
@Component
@RequiredArgsConstructor
public class InventoryProjection {

	private final InventoryRepository repository;

	// when creating a new container
	@EventHandler
	public void on(ContainerCreated containerCreated) {
		Inventory mt = new Inventory();
		mt.getKey().setZoneName(containerCreated.getCurrentZoneName());
		mt.getKey().setPortName(containerCreated.getCurrentPortName());
		mt.setAvailableContainers(1);
		mt.setLastUpdated(LocalDateTime.now());
		repository.save(mt);
	}


	// When reserving a container update origin zone/port available containers
	@EventHandler
	public void onOrigin(ContainerReserved containerReserved) {
		Inventory originInventory = repository.findById(
				new InventoryKey(containerReserved.getOrigZoneName()
						, containerReserved.getOrigPortName())
		).orElseGet(() -> null);

		if (originInventory != null && originInventory.getAvailableContainers() > 0) {
			originInventory.setAvailableContainers(originInventory.getAvailableContainers() - 1);
			repository.save(originInventory);
		}
	}

	// When reserving a container update destination zone/port forecast containers
	@EventHandler
	public void onDest(ContainerReserved containerReserved) {
		Inventory destInventory = repository.findById(
				new InventoryKey(containerReserved.getDestZoneName()
						, containerReserved.getDestPortName())
		).orElseGet(() -> null);
		if (destInventory != null) {
			destInventory.setForecastContainers(destInventory.getForecastContainers() + 1);
			repository.save(destInventory);
		}
	}

}

