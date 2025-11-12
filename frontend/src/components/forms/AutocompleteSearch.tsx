import { useState, useEffect, useRef } from "react";

type AutocompleteSearchProps = {
  data: any[];
  searchFields: string[];
  onFilter: (filteredData: any[]) => void;
  placeholder?: string;
};

export default function AutocompleteSearch({
  data,
  searchFields,
  onFilter,
  placeholder = "Search...",
}: AutocompleteSearchProps) {
  const [searchTerm, setSearchTerm] = useState(""); // user input
  const [suggestions, setSuggestions] = useState<string[]>([]); // autocomplete options
  const [showSuggestions, setShowSuggestions] = useState(false); // show or hide autocomplete options
  const [selectedIndex, setSelectedIndex] = useState(-1); // tracks which suggestion is selected
  const inputRef = useRef<HTMLInputElement>(null);
  const suggestionsRef = useRef<HTMLDivElement>(null);

  // Handle search and filtering
  useEffect(() => {
    if (!searchTerm.trim()) {
      onFilter(data);
      setSuggestions([]);
      return;
    }

    const lowerSearch = searchTerm.toLowerCase();
    
    // Filter data based on search term
    const filtered = data.filter((item) =>
      searchFields.some((field) => {
        const value = item[field];
        return value?.toString().toLowerCase().includes(lowerSearch);
      })
    );

    onFilter(filtered);

    // Generate suggestions from all matching values
    const uniqueSuggestions = new Set<string>();
    data.forEach((item) => {
      searchFields.forEach((field) => {
        const value = item[field]?.toString();
        if (value && value.toLowerCase().includes(lowerSearch)) {
          uniqueSuggestions.add(value);
        }
      });
    });

    setSuggestions(Array.from(uniqueSuggestions).slice(0, 8));
  }, [searchTerm, data, searchFields, onFilter]);

  
  // Handle keyboard navigation
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!showSuggestions || suggestions.length === 0) return;

    switch (e.key) {
      case "ArrowDown":
        e.preventDefault();
        setSelectedIndex((prev) =>
          prev < suggestions.length - 1 ? prev + 1 : prev
        );
        break;
      case "ArrowUp":
        e.preventDefault();
        setSelectedIndex((prev) => (prev > 0 ? prev - 1 : -1));
        break;
      case "Enter":
        e.preventDefault();
        if (selectedIndex >= 0) {
          setSearchTerm(suggestions[selectedIndex]);
          setShowSuggestions(false);
          setSelectedIndex(-1);
        }
        break;
      case "Escape":
        setShowSuggestions(false);
        setSelectedIndex(-1);
        break;
    }
  };

  // Handle clicking outside to close suggestions
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        suggestionsRef.current &&
        !suggestionsRef.current.contains(event.target as Node) &&
        inputRef.current &&
        !inputRef.current.contains(event.target as Node)
      ) {
        setShowSuggestions(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleSuggestionClick = (suggestion: string) => {
    setSearchTerm(suggestion);
    setShowSuggestions(false);
    setSelectedIndex(-1);
  };

  const handleClear = () => {
    setSearchTerm("");
    setShowSuggestions(false);
    setSelectedIndex(-1);
    inputRef.current?.focus();
  };

  return (
    <div className="relative w-full max-w-2xl mb-6">
      <div className="relative">
        <input
          ref={inputRef}
          type="text"
          value={searchTerm}
          onChange={(e) => {
            setSearchTerm(e.target.value);
            setShowSuggestions(true);
            setSelectedIndex(-1);
          }}
          onFocus={() => setShowSuggestions(true)}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          className="w-full px-4 py-3 pl-10 pr-10 text-sm border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        />
        
        {/* Search icon */}
        <svg
          className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
          />
        </svg>

        {/* Clear button */}
        {searchTerm && (
          <button
            onClick={handleClear}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        )}
      </div>

      {/* Suggestions dropdown */}
      {showSuggestions && suggestions.length > 0 && (
        <div
          ref={suggestionsRef}
          className="absolute z-10 w-full mt-1 bg-white border border-slate-300 rounded-lg shadow-lg max-h-60 overflow-y-auto"
        >
          {suggestions.map((suggestion, index) => (
            <button
              key={index}
              onClick={() => handleSuggestionClick(suggestion)}
              className={`w-full px-4 py-2 text-left text-sm hover:bg-blue-50 transition-colors ${
                index === selectedIndex ? "bg-blue-100" : ""
              }`}
            >
              {suggestion}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}