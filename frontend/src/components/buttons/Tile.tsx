import React from 'react';

interface TileProps {
  title: string;
  onClick: () => void;
}

const Tile: React.FC<TileProps> = ({ title, onClick }) => {
  return (
    <div 
      style={{
        border: '1px solid #ddd',
        padding: '20px',
        margin: '10px',
        cursor: 'pointer',
        borderRadius: '8px',
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
      }}
      onClick={onClick}
    >
      <h3>{title}</h3>
    </div>
  );
};

export default Tile;