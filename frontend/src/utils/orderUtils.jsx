const toTimestamp = (value) => {
    const timestamp = new Date(value || 0).getTime();
    return Number.isNaN(timestamp) ? 0 : timestamp;
};

const getOrderTimestamp = (order) => {
    if (!order) return 0;
    return Math.max(
        toTimestamp(order.createdAt),
        toTimestamp(order.updatedAt),
        toTimestamp(order.orderTime)
    );
};

export const sortOrdersByMostRecent = (orders = []) => [...orders].sort((a, b) => {
    const timestampDiff = getOrderTimestamp(b) - getOrderTimestamp(a);
    if (timestampDiff !== 0) return timestampDiff;
    return (b?.orderId || 0) - (a?.orderId || 0);
});

