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

import io.agilehandy.command.api.OpStatus;
import io.agilehandy.command.api.TransmitStatus;
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
import io.agilehandy.command.api.evt.ContainerCreated;
import io.agilehandy.command.api.evt.ContainerOpLoaded;
import io.agilehandy.command.api.evt.ContainerOpOffLoaded;
import io.agilehandy.command.api.evt.ContainerOpReleased;
import io.agilehandy.command.api.evt.ContainerOpReserved;
import io.agilehandy.command.api.evt.ContainerTransArrived;
import io.agilehandy.command.api.evt.ContainerTransBoarded;
import io.agilehandy.command.api.evt.ContainerTransDeparted;
import io.agilehandy.command.api.evt.ContainerTransOffBoarded;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class Container {

	@AggregateIdentifier
	UUID id;

	float size;
	float usedSize;

	String currentZoneName;
	String currentPortName;

	LocalDateTime createTimeStamp;

	boolean isShared;
	LocalDateTime assignmentDate;
	UUID shipmentId;
	String originZoneName;
	String originPortName;
	String destZoneName;
	String destPortName;

	OpStatus opStatus;
	LocalDateTime operationTimestamp;

	TransmitType transmitType;
	TransmitStatus transmitStatus;
	LocalDateTime transmitTimestamp;

	boolean isEmpty() {
		return this.usedSize == 0f;
	}

	boolean isFull() {
		return this.size == this.usedSize;
	}

	boolean spaceAvailable() {
		return this.usedSize < this.size;
	}

	boolean canReserve() {
		return (spaceAvailable()
				&& transmitStatus.getValue().equals(TransmitStatus.OFF_BOARDED.getValue())
				&& opStatus.getValue().equals(OpStatus.RELEASED.getValue()));
	}

	boolean canLoad() {
		return opStatus.getValue().equals(OpStatus.RESERVED.getValue())
				&& this.transmitStatus.getValue().equals(TransmitStatus.OFF_BOARDED.getValue())
				&& !isFull();
	}

	boolean canBoard() {
		return opStatus.getValue().equals(OpStatus.LOADED.getValue())
				&& this.transmitStatus.getValue().equals(TransmitStatus.OFF_BOARDED.getValue());
	}

	boolean canDepart() {
		return opStatus.getValue().equals(OpStatus.LOADED.getValue())
				&& this.transmitStatus.getValue().equals(TransmitStatus.BOARDED.getValue());
	}

	boolean canArrive() {
		return opStatus.getValue().equals(OpStatus.LOADED.getValue())
				&& this.transmitStatus.getValue().equals(TransmitStatus.DEPARTED.getValue());
	}

	boolean canOffBoard() {
		return opStatus.getValue().equals(OpStatus.LOADED.getValue())
				&& this.transmitStatus.getValue().equals(TransmitStatus.ARRIVED.getValue());
	}

	boolean canOffLoad() {
		return opStatus.getValue().equals(OpStatus.LOADED.getValue())
				&& this.transmitStatus.getValue().equals(TransmitStatus.OFF_BOARDED.getValue());
	}

	boolean canRelease() {
		return
				opStatus.getValue().equals(OpStatus.OFF_LOADED.getValue())
						&& this.transmitStatus.getValue().equals(TransmitStatus.OFF_BOARDED.getValue());
	}

	// creating a new container
	@CommandHandler
	public Container(ContainerCreateCommand containerCreateCommand){
		log.info("Command to create a new container");
		Assert.notNull(containerCreateCommand.getCurrentZoneName(),
				() ->"Must assign a container to a zone name");
		Assert.notNull(containerCreateCommand.getCurrentPortName(),
				() ->"Must assign a container to a facility center name");

		apply(new ContainerCreated(UUID.randomUUID(),
				containerCreateCommand.getSize(),
				containerCreateCommand.getCurrentZoneName(),
				containerCreateCommand.getCurrentPortName(),
				LocalDateTime.now()));
	}

	@EventSourcingHandler
	public void on(ContainerCreated event) {
		this.id = event.getId();
		this.size = event.getSize();
		this.createTimeStamp = event.getTimestamp();
		this.currentPortName = this.getCurrentPortName();
		this.currentZoneName = this.getCurrentZoneName();
		this.usedSize = 0f;
		this.opStatus = OpStatus.RELEASED;
		this.transmitStatus = TransmitStatus.OFF_BOARDED;
	}

	// reserve a container
	@CommandHandler
	public void reserve(ContainerOpReserveCommand containerBookCommand) {
		log.info("command to reserve a new container");
		Assert.state(canReserve(), () -> "Container is not available to reserve");
		apply (new ContainerOpReserved(containerBookCommand.getId(),
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
	public void on(ContainerOpReserved containerBooked) {
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

	//**********
	// Operation
	//**********

	// load a container
	@CommandHandler
	public void load(ContainerOpLoadCommand cmd) {
		Assert.state(canLoad(), () -> "Container is not ready for loading");
		apply(new ContainerOpLoaded(cmd.getId(),
				cmd.getUsedSize(),
				LocalDateTime.now()));
	}

	@EventSourcingHandler
	public void on(ContainerOpLoaded event) {
		this.opStatus = OpStatus.LOADED;
		this.operationTimestamp = event.getTimestamp();
		this.usedSize = event.getUsedSize();
	}

	// off-load a container
	@CommandHandler
	public void offLoad(ContainerOpOffLoadCommand cmd) {
		Assert.state(canOffLoad(), () -> "Container is not ready for off loading");
		apply(new ContainerOpOffLoaded(cmd.getId(),
				LocalDateTime.now()));
	}

	@EventSourcingHandler
	public void on(ContainerOpOffLoaded event) {
		this.opStatus = OpStatus.OFF_LOADED;
		this.operationTimestamp = event.getTimestamp();
	}

	// release a container
	@CommandHandler
	public void release(ContainerOpReleasedCommand cmd) {
		Assert.state(canRelease(), () -> "Container is not ready to be released");
		apply(new ContainerOpReleased(cmd.getId(),
				LocalDateTime.now()));
	}

	@EventSourcingHandler
	public void on(ContainerOpReleased event) {
		this.opStatus = OpStatus.RELEASED;
		this.operationTimestamp = event.getTimestamp();
	}

	//*************
	// TRANSPORTING
	//*************

	// board a container to a ship
	@CommandHandler
	public void board(ContainerTransBoardCommand cmd) {
		Assert.state(canBoard(), () -> "Container is not ready for boarding");
		apply (new ContainerTransBoarded(cmd.getId(),
				LocalDateTime.now()));
	}

	@EventSourcingHandler
	public void on(ContainerTransBoarded event) {
		this.transmitStatus = TransmitStatus.BOARDED;
		this.transmitTimestamp = event.getTimestamp();
	}

	// depart a container
	@CommandHandler
	public void depart(ContainerTransDepartCommand cmd) {
		Assert.state(canDepart(), () -> "Cannot depart container");
		apply (new ContainerTransDeparted(cmd.getId(),
				LocalDateTime.now()));
	}

	@EventSourcingHandler
	public void on(ContainerTransDeparted event) {
		this.transmitStatus = TransmitStatus.DEPARTED;
		this.transmitTimestamp = event.getTimestamp();
	}

	// arrive a container
	@CommandHandler
	public void arrive(ContainerTransArriveCommand cmd) {
		Assert.state(canArrive(), () -> "Cannot arrive container");
		apply (new ContainerTransArrived(cmd.getId(),
				LocalDateTime.now()));
	}


	@EventSourcingHandler
	public void on(ContainerTransArrived event) {
		this.transmitStatus = TransmitStatus.ARRIVED;
		this.transmitTimestamp = event.getTimestamp();
		this.currentZoneName = this.destZoneName;
		this.currentPortName = this.destPortName;
	}

	// off boarding a container to a ship
	@CommandHandler
	public void offBoard(ContainerTransOffBoardCommand cmd) {
		Assert.state(canOffBoard(), () -> "Container is not ready for off boarding");
		apply (new ContainerTransOffBoarded(cmd.getId(),
				LocalDateTime.now()));
	}

	@EventSourcingHandler
	public void on(ContainerTransOffBoarded event) {
		this.transmitStatus = TransmitStatus.OFF_BOARDED;
		this.transmitTimestamp = event.getTimestamp();
	}

}
