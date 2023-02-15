# Voting on Hydra - Project Charter

This document is a draft of a Project Charter for the Voting on Hydra Proof-of-Concept project. It builds upon [previous RFP document](https://docs.google.com/document/u/0/d/12AV_OnQm-DIZgy7WWkl4NpN30jbhB97LTgU2MEvjb_U/edit) which can be used as reference. It also assumes project will be run more or less according to [eXtreme Programming](http://www.extremeprogramming.org/) principles and practices, eg. in an open, iterative and highly collaborative way.

# Vision

## Goals

The goal of this project is to demonstrate the feasibility of **using Hydra Head to support the Catalyst voting process**, taking into account the existing voting system, the integration with client software, and operational costs incurred for running such a system.

## Non-Goals

-   It is not expected the solution built will be production-ready, but it could serve as a stepping stone towards deployment in production for future Catalyst funding rounds

-   In particular, it is not expected the solution will be fully integrated within the existing or evolved Catalyst architecture nor all possible clients (eg. wallets)

-   It is not expected the solution will encompass all of the Catalyst voting process.

# Mission

## Objectives

Within a time frame of about 5 months, we expect one of the following
answers to the feasibility question:

1.  **Yes**, Hydra Head can be used to support part X of Catalyst process, and here are the performances one can expect

2.  **No**, Hydra Head network is useless as far as Catalyst is concerned, and here are the reasons why

3.  **Maybe** it could work if we enhanced Hydra Head with some features.

## Deliverables

In order to achieve that goal, the project is expected to deliver the following deliverables have been identified. These are subject to revision from the team depending on how the project unfolds.

**Note that the word \"voting\" is used here in a very broad sense and designates whatever part of the Catalyst system the team will be focusing on.**


| **What**    | **Goal**                | **Deliverable**                    |
| =========== | ======================= | ================================== |
| Voting      | Provide a high-level    | Feature map or similar document    |
| scheme      | understanding of the    | describing voting system with      |
| arc         | system                  | documentation on the required      |
| hitecture   |                         | properties (size) and output       |
|-------------|-------------------------|------------------------------------|
| Hydra       | Design and implement    | Scripts & Configuration needed     |
| Sc          | typical Hydra Head      | for deploying various components   |
| affolding   | deployment              | of the solution (Hydra node,       |
|             | representative of       | Cardano nodes, clients)            |
|             | target system           |                                    |
|-------------|-------------------------|------------------------------------|
| Voting      | Implement voting        | Deployed system should be able     |
| logic       | logic                   | to run a complete voting round     |
|             |                         | and provide meaningful results     |
|-------------|-------------------------|------------------------------------|
| User        | Provide some            | Actual shape of the UI will        |
| Interface   | interface for end       | depend on what kind of actions     |
|             | users to interact       | is allowed                         |
|             | with the system         |                                    |
|-------------|-------------------------|------------------------------------|
| Load        | Provide reliable load   | Tools & configuration to be able   |
| tester      | testing tool and        | to run load tests and extract      |
|             | performance benchmark   | performance metrics                |
|             |                         |                                    |
|             |                         | Testing runs provide both          |
|             |                         | applicative and system-level       |
|             |                         | metrics                            |
|-------------|-------------------------|------------------------------------|
| E           | Provide answers to      | Final project report along with    |
| xperiment   | (or at least enough     | code, documentation, and           |
| results     | data points to          | relevant data.                     |
|             | answer) the             |                                    |
|             | [[Goals]                |                                    |
|             | {.underline}](#goals)   |                                    |
|             |                         |                                    |
|-------------|-------------------------|------------------------------------|

# People

## Stakeholders

This table identifies stakeholders with an interest in the progress and outcome of the project. Stakeholders will be invited to participate in the project\'s regular demonstration meeting.


| Role                | Interest                                           |
| --                  | --                                                 |
| Catalyst Leadership | Help Catalyst grow                                 |
| Cardano Foundation  | Grow Cardano open-source solutions                 |
| IOG Research        | Identify synergies with current &  future research |

## Team

This table identifies people taking part in the project and defines their roles & responsibilities.

|--------------------|--------------------------------------------------------------|
| Role               | Responsibilities                                             |
| --                 |                                                              |
| Customer           | Define what needs to be delivered for each increment         |
|                    |                                                              |
|                    | Clarify acceptance criteria & high-level tests               |
|                    |                                                              |
|                    | Answer \"business-domain\" questions from the team           |
|                    |                                                              |
|--------------------|--------------------------------------------------------------|
|                    | Collaborate with Customer to define increments               |
| Engineering        |                                                              |
|                    |                                                              |
|                    | Build solution incrementally and demonstrate progress/issues |
|                    |                                                              |
|                    |                                                              |
|--------------------|--------------------------------------------------------------|
| Hydra SME          | Provide technical expertise on Hydra Head solution           |
|                    |                                                              |
|                    | Add / review core features to the hydra-node                 |
|--------------------|--------------------------------------------------------------|
| Voting Researchers | Provide insights on alternative solution                     |
|                    |                                                              |
|                    | Explore new techniques to improve existing solution          |
|                    |                                                              |

# Process

While it\'s the full responsibility of the Team to define and update the details of the process, it is expected some basic properties shall be maintained over the course of the project:

1.  All development will be done in open-source, using whatever infrastructure is convenient for the team
    a.  This includes the source code of the project but also all documentation and communication insofar as they are not pertaining to any parties trade secrets
    b.  This also includes current status and roadmap of the project
2.  There will be a regular (eg. every 2-4 weeks) Demonstration meeting whereby the Team will demonstrate progress with actual working software to Stakeholders
