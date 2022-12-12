<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;

/**
 * @property int $Ula_id
 * @property int $Ar_id
 * @property int $created_at
 * @property int $deleted_at
 * @property int $Ul_id
 * @property int $updated_at
 */
class ULevel2article extends Model
{
    use SoftDeletes;
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'u_level2article';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'Ula_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'Ar_id', 'created_at', 'deleted_at', 'Ul_id', 'updated_at'
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
        'Ula_id' => 'int', 'Ar_id' => 'int', 'created_at' => 'timestamp', 'deleted_at' => 'timestamp', 'Ul_id' => 'int', 'updated_at' => 'timestamp'
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

    public function ul_articles()
    {
        return $this->belongsTo(MArticle::class,  'Ar_id');
    }

    public function ul_levels()
    {
        return $this->belongsTo(ULevels::class,  'Ul_id');
    }
}
