<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

/**
 * @property int    $Cat_id
 * @property int    $Cat_parent_id
 * @property int    $created_at
 * @property int    $deleted_at
 * @property int    $updated_at
 * @property string $Cat_name
 */
class MCategory extends Model
{
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'm_category';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'Cat_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'Cat_name', 'Cat_parent_id', 'created_at', 'deleted_at', 'updated_at'
    ];

    /**
     * The attributes excluded from the model's JSON form.
     *
     * @var array
     */
    protected $hidden = [];

    /**
     * The attributes that should be casted to native types.
     *
     * @var array
     */
    protected $casts = [
        'Cat_id' => 'int', 'Cat_name' => 'string', 'Cat_parent_id' => 'int', 'created_at' => 'timestamp', 'deleted_at' => 'timestamp', 'updated_at' => 'timestamp'
    ];

    /**
     * The attributes that should be mutated to dates.
     *
     * @var array
     */
    protected $dates = [
        'created_at', 'deleted_at', 'updated_at'
    ];

    /**
     * Indicates if the model should be timestamped.
     *
     * @var boolean
     */
    public $timestamps = true;

    // Scopes...

    // Functions ...

    // Relations ...

    public function parent()
    {
        return $this->belongsTo(MCategory::class, 'Cat_parent_id', 'Cat_id');
    }

    public function children()
    {
        return $this->hasMany(MArticle::class, 'Cat_parent_id', 'Cat_id');
    }

    public function articles()
    {
        return $this->hasMany(MArticle::class, 'Cat_id');
    }
}
