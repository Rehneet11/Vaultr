import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '10s', target: 50 },
        { duration: '30s', target: 50 },
        { duration: '10s', target: 0 },
    ],
};

// Helper function to generate a unique idempotency key for every request
function generateIdempotencyKey() {
    return 'k6-test-' + Date.now() + '-' + Math.floor(Math.random() * 1000000);
}

export default function () {
    const url = 'http://localhost:8080/api/transactions';

    const payload = JSON.stringify({
        sourceWalletId: "1259121142065528832",
        destinationWalletId: "1259121205315633153",
        amount: Math.floor(Math.random() * 5) + 1
    });

    // Inject the required header here
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Idempotency-Key': generateIdempotencyKey()
        },
    };

    const res = http.post(url, payload, params);

    // If it fails again, we want to know why immediately
    if (__ITER === 0 && res.status !== 200 && res.status !== 201) {
        console.log("Spring Boot says: " + res.body);
    }

    check(res, {
        'status is 200 or 201': (r) => r.status === 200 || r.status === 201 || r.status === 400,
    });

    sleep(0.1);
}