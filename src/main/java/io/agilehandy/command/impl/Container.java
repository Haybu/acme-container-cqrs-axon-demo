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


package io.agilehandy.command.impl;

import io.agilehandy.command.api.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.common.Assert;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.axonframework.modelling.command.AggregateLifecycle.*;

/**
 * @author Haytham Mohamed
 **/
@Aggregate
@Data
@NoArgsConstructor
public class Container {

	@AggregateIdentifier
	UUID id;

	Float size;
	Float usedSize;
	String currentZoneName;
	String currentPortName;

	Boolean isAssigned;
	Boolean isShared;
	LocalDateTime assignmentDate;
	UUID shipmentId;
	String originZoneName;
	String originPortName;
	String destZoneName;
	String destPortName;

	OpStatus opStatus;
	LocalDateTime opTimestamp;

	TransitType transitType;
	TransitStatus transitStatus;
	LocalDateTime transitTimestamp;

	Boolean isEmpty() {
		return (!this.getIsAssigned() && usedSize == 0);
	}

	Boolean isAvailable() {
		return (isEmpty() && transitStatus == TransitStatus.PARKED && opStatus == OpStatus.NO_OP);
	}

	@CommandHandler
	public Container(ContainerCreateCommand containerCreateCommand){
		Assert.notNull(containerCreateCommand.getCurrentZoneName(),
				() ->"Must assign a container to a zone name");
		Assert.notNull(containerCreateCommand.getCurrentPortName(),
				() ->"Must assign a container to a facility center name");

		apply(new ContainerCreated(UUID.randomUUID(), 120f, "port-1", "zone-1"));
	}

	@EventSourcingHandler
	public void on(ContainerCreated containerCreated) {
		this.id = containerCreated.getId();
		this.size = containerCreated.getSize();
		this.currentPortName = this.getCurrentPortName();
		this.currentZoneName = this.getCurrentZoneName();
		this.usedSize = 0f;
		this.opStatus = OpStatus.NO_OP;
		this.transitStatus = TransitStatus.PARKED;
	}

	@CommandHandler
	public void book(ContainerBookCommand containerBookCommand) {
		Assert.notNull(containerBookCommand.getId(), () -> "Container ID should be known to book");
		// TODO: check others here.

		apply (new ContainerBooked(containerBookCommand.getId(), containerBookCommand.getShipmentId(),
				containerBookCommand.getOriginZoneName(), containerBookCommand.getOriginPortName(),
				containerBookCommand.getDestZoneName(), containerBookCommand.getDestPortName()));
	}

	@EventSourcingHandler
	public void on(ContainerBooked containerBooked) {
		this.shipmentId = containerBooked.getShipmentId();
		this.originZoneName = containerBooked.getOriginPortName();
		this.originPortName = containerBooked.getOriginPortName();
		this.destZoneName = containerBooked.getDestZoneName();
		this.destPortName = containerBooked.getDestPortName();
		if(this.isAssigned)
			this.isShared = true;
		else
			this.isShared = false;
		this.isAssigned = true;
	}

	@CommandHandler
	public void operate(ContainerOpCommand containerLoadCommand) {
		apply(new ContainerOperated(containerLoadCommand.getId(),
				OpStatus.LOADING_STARTED,
				containerLoadCommand.getUsedSize(),
				LocalDateTime.now()));

	}

	@EventSourcingHandler
	public void on(ContainerOperated containerLoaded) {
		this.opStatus = containerLoaded.getOpStatus();
		this.opTimestamp = containerLoaded.getLoadTimestamp();
		if(this.opStatus == OpStatus.LOADING_COMPLETED)
			this.usedSize = containerLoaded.getUsedSize();
	}

	@CommandHandler
	public void transit(ContainerTransitCommand containerTransitCommand) {
		// TODO: validation
		apply (new ContainerTranisted(containerTransitCommand.getId(),
				containerTransitCommand.getOriginZoneName(),
				containerTransitCommand.getOriginPortName(),
				containerTransitCommand.getDestZoneName(),
				containerTransitCommand.getDestPortName(),
				containerTransitCommand.getTransitType(),
				containerTransitCommand.getTransitStatus(),
				LocalDateTime.now()));
	}

	@EventSourcingHandler
	public void on(ContainerTranisted containerTranisted) {
		this.transitStatus = containerTranisted.getTransitStatus();
		this.transitType = containerTranisted.getTransitType();
		if (this.getTransitStatus() == TransitStatus.TRANSIT) {
			this.originZoneName = containerTranisted.getOriginZoneName();
			this.originPortName = containerTranisted.getOriginPortName();
			this.destZoneName = containerTranisted.getDestZoneName();
			this.destPortName = containerTranisted.getDestPortName();
		}
		this.transitTimestamp = containerTranisted.getTransitTimestamp();
	}


}
