pragma solidity ^0.4.0;

contract Storage
{
    uint public gasLimit;
    uint public gasPrice;
    uint public updateTime;
    struct Record
    {
        uint32 nonce;
        bytes value;
    }
    mapping(address => Record) public records;
    address public owner;
    modifier isOwner()
    {
        require(msg.sender == owner);
        _;
    }
    function Storage(uint _gasLimit, uint _gasPrice) payable public
    {
        owner = msg.sender;
        gasLimit = _gasLimit;
        gasPrice = _gasPrice;
        updateTime = block.timestamp;
    }
    function setGasLimit(uint _gasLimit) public isOwner
    {
        require(block.timestamp > updateTime + 1 minutes);
        gasLimit = _gasLimit;
        updateTime = block.timestamp;
    }   
    function setGasPrice(uint _gasPrice) public isOwner
    {
        require(block.timestamp > updateTime + 1 minutes);
        gasPrice = _gasPrice;
        updateTime = block.timestamp;
    }
    function resetRecord(address _addr) isOwner public
    {
        records[_addr].nonce = 1;
    }
    function writeRecord(address _from, uint32 _nonce, bytes32 _r, bytes32 _s, bytes _inputData) public
    {
        require(tx.gasprice <= gasPrice);
        require(records[_from].nonce > 0);
        require(_nonce > records[_from].nonce);
        require((ecrecover(keccak256(_nonce, _inputData), 27, _r, _s) == _from) || 
                (ecrecover(keccak256(_nonce, _inputData), 28, _r, _s) == _from));
        records[_from] = Record(_nonce, _inputData);
        msg.sender.transfer(gasLimit * tx.gasprice);
    }
    function () payable public {}
}
