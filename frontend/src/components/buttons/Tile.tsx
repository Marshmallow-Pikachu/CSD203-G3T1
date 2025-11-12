import React from "react";

export default function Tile({
    title,
    icon: Icon,
    onClick,
}: {
    title: string;
    icon?: React.ElementType;
    onClick: () => void;
}) {
    if (!title) return <div style={{ width: "100%", height: "100%" }} />; // Blank slot
    return (
        <button
            onClick={onClick}
            style={{
                width: "100%",
                height: "100%",
                borderRadius: "12px",
                border: "1px solid #e5e7eb",
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                justifyContent: "center",
                background: "#fff",
                boxShadow: "0 1px 8px rgba(0,0,0,0.03)"
            }}
        >
            {Icon && <Icon size={32} style={{ marginBottom: 8 }} />}
            <span style={{ fontWeight: 500 }}>{title}</span>
        </button>
    );
}