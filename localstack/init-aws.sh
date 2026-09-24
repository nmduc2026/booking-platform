#!/usr/bin/env bash
set -euo pipefail

echo "Creating SQS queues..."

awslocal sqs create-queue --queue-name booking-events
awslocal sqs create-queue --queue-name booking-events-dlq
awslocal sqs create-queue --queue-name booking-events-realtime
awslocal sqs create-queue --queue-name booking-events-realtime-dlq
awslocal sqs create-queue --queue-name payment-events
awslocal sqs create-queue --queue-name payment-events-dlq
awslocal sqs create-queue --queue-name notification-events
awslocal sqs create-queue --queue-name notification-events-dlq

echo "SQS queues ready."
