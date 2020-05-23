# CxFlow Helm Chart

Kubernetes chart to deploy cxflow.

## Introduction

This chart bootstraps cxflow deployment on a [Kubernetes](http://kubernetes.io) cluster using the [Helm](https://helm.sh) package manager.

## Prerequisites

- Kubernetes 1.9+

## Kubernetes Addons

aws-alb-ingress-controller
```bash
helm upgrade --install aws-alb-ingress-controller --set clusterName=CLUSTER_NAME
--set autoDiscoverAwsRegion=true --set autoDiscoverAwsVpcID=true
incubator/aws-alb-ingress-controller
```

external-dns
```bash
helm upgrade --install external-dns --set provider=aws --set aws.zoneType=public 
--set txtOwnerId=HOSTED_ZONE_IDENTIFIER --set domainFilters[0]=HOSTED_ZONE_NAME --set policy=sync 
bitnami/external-dns
```

## Installing the Chart
To install the chart with the release name `cxflow` into `default`:

```bash
helm upgrade --install cxflow 
--set ingress.hosts[0].host=HOSTNAME --set ingress.hosts[0].paths[0]="/*" 
.
```

The command deploys cxflow on the Kubernetes cluster.

> **Tip**: List all releases using `helm list`

## Uninstalling the Chart

To uninstall the `cxflow` deployment:

```console
$ helm uninstall cxflow
```

The command removes all the Kubernetes components associated with the chart and deletes the release.
