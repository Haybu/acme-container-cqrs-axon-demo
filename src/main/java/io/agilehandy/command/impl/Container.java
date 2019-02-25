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

	Boolean isShared;
	LocalDateTime assignmentDate;
	UUID shipmentId;
	String originZoneName;
	String originPortName;
	String destZoneName;
	String destPortName;

	OpStatus opStatus;
	LocalDateTime opTimestamp;

	TransmitType transmitType;
	TransmitStatus transmitStatus;
	LocalDateTime transitTimestamp;

	boolean isEmpty() {
		return this.usedSize == 0;
	}

	boolean isFull() {
		return this.size == this.usedSize;
	}

	boolean canReserve() {
		return (isEmpty()
				&& transmitStatus == TransmitStatus.OFF_BOARDED
				&& opStatus == OpStatus.RELEASED);
	}

	boolean canLoad() {
		return opStatus == OpStatus.RESERVED && this.transmitStatus == TransmitStatus.OFF_BOARDED
				&& !isFull();
	}

	boolean canBoard() {
		return opStatus == OpStatus.LOADED && this.transmitStatus == TransmitStatus.OFF_BOARDED;
	}

	boolean canDepart() {
		return opStatus == OpStatus.LOADED && this.transmitStatus == TransmitStatus.BOARDED;
	}

	boolean canArrive() {
		return opStatus == OpStatus.LOADED && this.transmitStatus == TransmitStatus.DEPARTED;
	}

	boolean canOffBoard() {
		return opStatus == OpStatus.LOADED && this.transmitStatus == TransmitStatus.ARRIVED;
	}

	boolean canOffLoad() {
		return opStatus == OpStatus.LOADED && this.transmitStatus == TransmitStatus.OFF_BOARDED;
	}

	boolean canRelease() {
		return opStatus == OpStatus.OFF_LOADED && this.transmitStatus == TransmitStatus.OFF_BOARDED;
	}

	// creating a new container
	@CommandHandler
	public Container(ContainerCreateCommand containerCreateCommand){
		Assert.notNull(containerCreateCommand.getCurrentZoneName(),
				() ->"Must assign a container to a zone name");
		Assert.notNull(containerCreateCommand.getCurrentPortName(),
				() ->"Must assign a container to a facility center name");

		apply(new ContainerCreated(UUID.randomUUID(),
				containerCreateCommand.getSize(),
				containerCreateCommand.getCurrentZoneName(),
				containerCreateCommand.getCurrentPortName()));
	}

	@EventSourcingHandler
	public void on(ContainerCreated containerCreated) {
		this.id = containerCreated.getId();
		this.size = containerCreated.getSize();
		this.currentPortName = this.getCurrentPortName();
		this.currentZoneName = this.getCurrentZoneName();
		this.usedSize = 0f;
		this.opStatus = OpStatus.RELEASED;
		this.transmitStatus = TransmitStatus.OFF_BOARDED;
	}

	// reserve a container
	@CommandHandler
	public void reserve(ContainerReserveCommand containerBookCommand) {
		Assert.state(!canReserve(), () -> "Container is not available to reserve");
		apply (new ContainerReserved(containerBookCommand.getId(),
				LocalDateTime.now(),
				containerBookCommand.getShipmentId(),
				containerBookCommand.getTransitType(),
				containerBookCommand.getDestZoneName(),
				containerBookCommand.getDestPortName(),
				this.currentZoneName,
				this.currentPortName
				)
		);
	}

	@EventSourcingHandler
	public void on(ContainerReserved containerBooked) {
		this.shipmentId = containerBooked.getShipmentId();
		this.originZoneName = this.currentZoneName;
		this.originPortName = this.currentPortName;
		this.destZoneName = containerBooked.getDestZoneName();
		this.destPortName = containerBooked.getDestPortName();
		if(this.opStatus == OpStatus.RESERVED) { // if previously reserved, then share it with another customer
			this.isShared = true;
		} else {
			this.isShared = false;
		}
		this.opStatus = OpStatus.RESERVED;
	}

	// load a container
	@CommandHandler
	public void load(ContainerOpCommand containerLoadCommand) {
		Assert.state(!canLoad(), () -> "Container is not ready for loading");
		apply(new ContainerOperated(containerLoadCommand.getId(),
				OpStatus.LOADED,
				containerLoadCommand.getUsedSize(),
				LocalDateTime.now()));
	}

	// off-load a container
	@CommandHandler
	public void offLoad(ContainerOpCommand containerLoadCommand) {
		Assert.state(!canOffLoad(), () -> "Container is not ready for off loading");
		apply(new ContainerOperated(containerLoadCommand.getId(),
				OpStatus.OFF_LOADED,
				containerLoadCommand.getUsedSize(),
				LocalDateTime.now()));
	}

	// release a container
	@CommandHandler
	public void release(ContainerOpCommand containerLoadCommand) {
		Assert.state(!canRelease(), () -> "Container is not ready to be released");
		apply(new ContainerOperated(containerLoadCommand.getId(),
				OpStatus.RELEASED,
				containerLoadCommand.getUsedSize(),
				LocalDateTime.now()));
	}

	@EventSourcingHandler
	public void on(ContainerOperated containerLoaded) {
		this.opStatus = containerLoaded.getOpStatus();
		this.opTimestamp = containerLoaded.getLoadTimestamp();
		if(this.opStatus == OpStatus.LOADED) {
			this.usedSize = containerLoaded.getUsedSize();
		}
	}

	// board a container to a ship
	@CommandHandler
	public void board(ContainerTransmitCommand containerTransitCommand) {
		Assert.state(!canBoard(), () -> "Container is not ready for boarding");
		apply (new ContainerTransmited(containerTransitCommand.getId(),
				TransmitStatus.BOARDED,
				LocalDateTime.now()));
	}

	// depart a container
	@CommandHandler
	public void depart(ContainerTransmitCommand containerTransitCommand) {
		Assert.state(!canDepart(), () -> "Container is not ready to depart");
		apply (new ContainerTransmited(containerTransitCommand.getId(),
				TransmitStatus.DEPARTED,
				LocalDateTime.now()));
	}

	// arrive a container
	@CommandHandler
	public void arrive(ContainerTransmitCommand containerTransitCommand) {
		Assert.state(!canArrive(), () -> "Cannot arrive container");
		apply (new ContainerTransmited(containerTransitCommand.getId(),
				TransmitStatus.ARRIVED,
				LocalDateTime.now()));
	}

	// off board a container from a ship
	@CommandHandler
	public void offBoard(ContainerTransmitCommand containerTransitCommand) {
		Assert.state(!canOffBoard(), () -> "Cannot off board container");
		apply (new ContainerTransmited(containerTransitCommand.getId(),
				TransmitStatus.OFF_BOARDED,
				LocalDateTime.now()));
	}

	@EventSourcingHandler
	public void on(ContainerTransmited containerTransmited) {
		this.transmitStatus = containerTransmited.getTransitStatus();
		this.transitTimestamp = containerTransmited.getTransitTimestamp();

		if (transmitStatus == TransmitStatus.ARRIVED) {
			this.currentZoneName = this.destZoneName;
			this.currentPortName = this.destPortName;
		}
	}

}
